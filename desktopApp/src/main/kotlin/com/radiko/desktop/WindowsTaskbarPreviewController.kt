package com.radiko.desktop

import com.radiko.i18n.AppLocalizer
import com.radiko.settings.AppLanguage
import com.radiko.station.logoUrl
import com.radiko.ui.viewmodel.PlayerState
import com.sun.jna.CallbackReference
import com.sun.jna.Native
import com.sun.jna.Platform
import com.sun.jna.Pointer
import com.sun.jna.Structure
import com.sun.jna.platform.win32.COM.Unknown
import com.sun.jna.platform.win32.GDI32
import com.sun.jna.platform.win32.Guid
import com.sun.jna.platform.win32.Ole32
import com.sun.jna.platform.win32.User32
import com.sun.jna.platform.win32.WinDef.HBITMAP
import com.sun.jna.platform.win32.WinDef.HICON
import com.sun.jna.platform.win32.WinDef.HINSTANCE
import com.sun.jna.platform.win32.WinDef.HWND
import com.sun.jna.platform.win32.WinDef.LPARAM
import com.sun.jna.platform.win32.WinDef.LRESULT
import com.sun.jna.platform.win32.WinDef.WPARAM
import com.sun.jna.platform.win32.WinNT.HRESULT
import com.sun.jna.platform.win32.WinUser
import com.sun.jna.platform.win32.WinGDI
import com.sun.jna.ptr.IntByReference
import com.sun.jna.ptr.PointerByReference
import java.awt.Color
import java.awt.EventQueue
import java.awt.RenderingHints
import java.awt.Window
import java.awt.geom.Path2D
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.File
import java.net.URL
import java.nio.ByteOrder
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import javax.imageio.ImageIO

class WindowsTaskbarPreviewController(
    private val onPreviousStation: () -> Unit,
    private val onTogglePlayback: () -> Unit,
    private val onNextStation: () -> Unit,
) {
    private val iconDirectory = File(System.getProperty("java.io.tmpdir"), "radikall-taskbar")
    private val imageLoader = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "radikall-taskbar-preview").apply {
            isDaemon = true
        }
    }
    private val logoCache = ConcurrentHashMap<String, BufferedImage>()
    private val fallbackPreviewImage = loadBundledImage("/logo2.png") ?: createFallbackPreviewImage()

    @Volatile
    private var currentThumbnailImage: BufferedImage = fallbackPreviewImage

    @Volatile
    private var currentLogoUrl: String? = null
    private var currentLanguage: AppLanguage = AppLanguage.ENGLISH

    private var window: Window? = null
    private var hwnd: HWND? = null
    private var taskbarList: TaskbarList3? = null
    private var originalWndProc: Pointer? = null
    private var windowProc: WinUser.WindowProc? = null
    private var taskbarButtonCreatedReceived = false
    private var buttonsAdded = false
    private var oleInitialized = false

    private var previousIcon: HICON? = null
    private var playIcon: HICON? = null
    private var pauseIcon: HICON? = null
    private var nextIcon: HICON? = null

    private var currentState: PlayerState = PlayerState()

    fun attach(window: Window) {
        if (!Platform.isWindows()) {
            return
        }
        this.window = window

        runOnEdt {
            val pointer = Native.getWindowPointer(window) ?: return@runOnEdt
            val nativeHwnd = HWND(pointer)
            hwnd = nativeHwnd

            initializeCom()
            initializeTaskbarList()
            configureIconicPreview(nativeHwnd)
            installWindowProc(nativeHwnd)
            ensureButtonIcons()
            refreshThumbnailSource(currentState)
            update(currentState)
        }
    }

    fun update(
        state: PlayerState,
        language: AppLanguage = currentLanguage,
    ) {
        currentState = state
        currentLanguage = language
        refreshThumbnailSource(state)

        if (!Platform.isWindows()) {
            return
        }

        runOnEdt {
            updateTaskbarButtons()
            updateThumbnailTooltip()
            invalidateTaskbarPreview()
        }
    }

    fun dispose() {
        if (!Platform.isWindows()) {
            return
        }

        runOnEdt {
            val nativeHwnd = hwnd
            if (nativeHwnd != null && originalWndProc != null) {
                User32.INSTANCE.SetWindowLongPtr(nativeHwnd, GWL_WNDPROC, originalWndProc)
            }
            originalWndProc = null
            windowProc = null

            releaseButtonIcons()
            taskbarList?.Release()
            taskbarList = null
            buttonsAdded = false
            taskbarButtonCreatedReceived = false
            hwnd = null
            window = null

            if (oleInitialized) {
                Ole32.INSTANCE.CoUninitialize()
                oleInitialized = false
            }
        }

        imageLoader.shutdownNow()
    }

    private fun initializeCom() {
        val result = Ole32.INSTANCE.CoInitializeEx(Pointer.NULL, Ole32.COINIT_APARTMENTTHREADED)
        oleInitialized = result.toInt() >= 0
    }

    private fun initializeTaskbarList() {
        if (taskbarList != null) {
            return
        }

        val pointer = PointerByReference()
        val result = Ole32.INSTANCE.CoCreateInstance(
            CLSID_TASKBAR_LIST,
            Pointer.NULL,
            CLSCTX_INPROC_SERVER,
            IID_TASKBAR_LIST3,
            pointer,
        )
        if (result.toInt() < 0 || pointer.value == null) {
            println("Radikall taskbar: CoCreateInstance failed with HRESULT=0x${result.toInt().toUInt().toString(16)}")
            return
        }

        taskbarList = TaskbarList3(pointer.value).also { list ->
            list.hrInit()
        }
    }

    private fun configureIconicPreview(hwnd: HWND) {
        val enabled = IntByReference(1)
        DwmApi.INSTANCE.DwmSetWindowAttribute(hwnd, DWMWA_FORCE_ICONIC_REPRESENTATION, enabled.pointer, Int.SIZE_BYTES)
        DwmApi.INSTANCE.DwmSetWindowAttribute(hwnd, DWMWA_HAS_ICONIC_BITMAP, enabled.pointer, Int.SIZE_BYTES)
    }

    private fun installWindowProc(hwnd: HWND) {
        if (windowProc != null) {
            return
        }

        originalWndProc = Pointer.createConstant(
            User32.INSTANCE.GetWindowLongPtr(hwnd, GWL_WNDPROC).toLong(),
        )

        val callback = WinUser.WindowProc { windowHandle, message, wParam, lParam ->
            when (message) {
                taskbarButtonCreatedMessage -> {
                    taskbarButtonCreatedReceived = true
                    updateTaskbarButtons()
                    updateThumbnailTooltip()
                    invalidateTaskbarPreview()
                    LRESULT(0)
                }

                WM_COMMAND -> {
                    if (handleThumbButtonCommand(wParam)) {
                        LRESULT(0)
                    } else {
                        callOriginalWindowProc(windowHandle, message, wParam, lParam)
                    }
                }

                WM_DWMSENDICONICTHUMBNAIL -> {
                    provideIconicThumbnail(lParam)
                    LRESULT(0)
                }

                else -> callOriginalWindowProc(windowHandle, message, wParam, lParam)
            }
        }

        windowProc = callback
        User32.INSTANCE.SetWindowLongPtr(
            hwnd,
            GWL_WNDPROC,
            CallbackReference.getFunctionPointer(callback),
        )
    }

    private fun callOriginalWindowProc(
        hwnd: HWND,
        message: Int,
        wParam: WPARAM,
        lParam: LPARAM,
    ): LRESULT {
        val original = originalWndProc
        return if (original != null) {
            User32.INSTANCE.CallWindowProc(original, hwnd, message, wParam, lParam)
        } else {
            User32.INSTANCE.DefWindowProc(hwnd, message, wParam, lParam)
        }
    }

    private fun handleThumbButtonCommand(wParam: WPARAM): Boolean {
        val combined = wParam.toInt()
        val commandId = combined and 0xFFFF
        val notificationCode = (combined ushr 16) and 0xFFFF
        if (notificationCode != THBN_CLICKED) {
            return false
        }

        EventQueue.invokeLater {
            when (commandId) {
                BUTTON_PREVIOUS -> onPreviousStation()
                BUTTON_PLAY_PAUSE -> onTogglePlayback()
                BUTTON_NEXT -> onNextStation()
            }
        }
        return true
    }

    private fun updateTaskbarButtons() {
        val nativeHwnd = hwnd ?: return
        val taskbar = taskbarList ?: return
        if (!taskbarButtonCreatedReceived) {
            return
        }

        val buttons = createThumbButtons(currentState)
        if (!buttonsAdded) {
            val result = taskbar.thumbBarAddButtons(nativeHwnd, buttons)
            if (result.toInt() >= 0) {
                buttonsAdded = true
            } else {
                println("Radikall taskbar: ThumbBarAddButtons failed with HRESULT=0x${result.toInt().toUInt().toString(16)}")
            }
        } else {
            val result = taskbar.thumbBarUpdateButtons(nativeHwnd, buttons)
            if (result.toInt() < 0) {
                println("Radikall taskbar: ThumbBarUpdateButtons failed with HRESULT=0x${result.toInt().toUInt().toString(16)}")
            }
        }
    }

    private fun updateThumbnailTooltip() {
        val nativeHwnd = hwnd ?: return
        val taskbar = taskbarList ?: return
        if (!taskbarButtonCreatedReceived) {
            return
        }

        val title = currentState.currentProgram?.title?.takeIf { it.isNotBlank() }
            ?: currentState.currentStation?.name
            ?: "Radikall"
        taskbar.setThumbnailTooltip(nativeHwnd, title)
    }

    private fun refreshThumbnailSource(state: PlayerState) {
        val targetLogoUrl = state.currentStation?.logoUrl
        if (targetLogoUrl == currentLogoUrl) {
            return
        }

        currentLogoUrl = targetLogoUrl
        currentThumbnailImage = targetLogoUrl?.let(logoCache::get) ?: fallbackPreviewImage
        invalidateTaskbarPreview()

        if (targetLogoUrl == null || logoCache.containsKey(targetLogoUrl)) {
            return
        }

        imageLoader.execute {
            val loadedImage = loadRemoteImage(targetLogoUrl) ?: return@execute
            logoCache[targetLogoUrl] = loadedImage
            if (currentLogoUrl == targetLogoUrl) {
                currentThumbnailImage = loadedImage
                runOnEdt {
                    invalidateTaskbarPreview()
                }
            }
        }
    }

    private fun provideIconicThumbnail(lParam: LPARAM) {
        val nativeHwnd = hwnd ?: return
        val requestedWidth = extractHighWord(lParam.toLong()).coerceAtLeast(1)
        val requestedHeight = extractLowWord(lParam.toLong()).coerceAtLeast(1)
        val thumbnail = buildContainedThumbnail(requestedWidth, requestedHeight, currentThumbnailImage)
        val bitmap = createBitmapHandle(thumbnail) ?: return
        try {
            DwmApi.INSTANCE.DwmSetIconicThumbnail(nativeHwnd, bitmap, 0)
        } finally {
            GDI32.INSTANCE.DeleteObject(bitmap)
        }
    }

    private fun invalidateTaskbarPreview() {
        val nativeHwnd = hwnd ?: return
        DwmApi.INSTANCE.DwmInvalidateIconicBitmaps(nativeHwnd)
    }

    @Suppress("UNCHECKED_CAST")
    private fun createThumbButtons(state: PlayerState): Array<THUMBBUTTON> {
        val buttons = THUMBBUTTON().toArray(3) as Array<THUMBBUTTON>
        val stationSelected = state.currentStation != null
        val strings = AppLocalizer.strings(currentLanguage)

        buttons[0].apply {
            dwMask = THB_FLAGS or THB_ICON or THB_TOOLTIP
            iId = BUTTON_PREVIOUS
            hIcon = previousIcon
            setTip(strings.previousStation)
            dwFlags = if (stationSelected) THBF_ENABLED else THBF_DISABLED
            write()
        }
        buttons[1].apply {
            dwMask = THB_FLAGS or THB_ICON or THB_TOOLTIP
            iId = BUTTON_PLAY_PAUSE
            hIcon = if (state.isPlaying) pauseIcon else playIcon
            setTip(if (state.isPlaying) strings.pause else strings.play)
            dwFlags = if (stationSelected) THBF_ENABLED else THBF_DISABLED
            write()
        }
        buttons[2].apply {
            dwMask = THB_FLAGS or THB_ICON or THB_TOOLTIP
            iId = BUTTON_NEXT
            hIcon = nextIcon
            setTip(strings.nextStation)
            dwFlags = if (stationSelected) THBF_ENABLED else THBF_DISABLED
            write()
        }

        return buttons
    }

    private fun ensureButtonIcons() {
        if (previousIcon != null && playIcon != null && pauseIcon != null && nextIcon != null) {
            return
        }

        iconDirectory.mkdirs()
        previousIcon = loadIcon("thumb-prev.ico", drawThumbButtonIcon(IconType.PREVIOUS))
        playIcon = loadIcon("thumb-play.ico", drawThumbButtonIcon(IconType.PLAY))
        pauseIcon = loadIcon("thumb-pause.ico", drawThumbButtonIcon(IconType.PAUSE))
        nextIcon = loadIcon("thumb-next.ico", drawThumbButtonIcon(IconType.NEXT))
    }

    private fun loadIcon(fileName: String, image: BufferedImage): HICON? {
        val targetFile = File(iconDirectory, fileName)
        if (!targetFile.exists()) {
            writeIco(targetFile, image)
        }
        val handle = User32.INSTANCE.LoadImage(
            HINSTANCE(),
            targetFile.absolutePath,
            IMAGE_ICON,
            32,
            32,
            LR_LOADFROMFILE,
        ) ?: return null
        return HICON(handle.pointer)
    }

    private fun releaseButtonIcons() {
        listOf(previousIcon, playIcon, pauseIcon, nextIcon).forEach { icon ->
            if (icon != null) {
                User32.INSTANCE.DestroyIcon(icon)
            }
        }
        previousIcon = null
        playIcon = null
        pauseIcon = null
        nextIcon = null
    }

    private fun buildContainedThumbnail(
        width: Int,
        height: Int,
        source: BufferedImage,
    ): BufferedImage {
        val output = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
        val graphics = output.createGraphics()
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC)
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

        val sourceWidth = source.width.coerceAtLeast(1)
        val sourceHeight = source.height.coerceAtLeast(1)
        val scale = minOf(width.toFloat() / sourceWidth, height.toFloat() / sourceHeight)
        val drawWidth = (sourceWidth * scale).toInt().coerceAtLeast(1)
        val drawHeight = (sourceHeight * scale).toInt().coerceAtLeast(1)
        val drawX = (width - drawWidth) / 2
        val drawY = (height - drawHeight) / 2
        graphics.drawImage(source, drawX, drawY, drawWidth, drawHeight, null)
        graphics.dispose()
        return output
    }

    private fun createBitmapHandle(image: BufferedImage): HBITMAP? {
        val bitmapInfo = WinGDI.BITMAPINFO()
        bitmapInfo.bmiHeader.biSize = bitmapInfo.bmiHeader.size()
        bitmapInfo.bmiHeader.biWidth = image.width
        bitmapInfo.bmiHeader.biHeight = -image.height
        bitmapInfo.bmiHeader.biPlanes = 1
        bitmapInfo.bmiHeader.biBitCount = 32
        bitmapInfo.bmiHeader.biCompression = WinGDI.BI_RGB

        val pixelPointer = PointerByReference()
        val bitmap = GDI32.INSTANCE.CreateDIBSection(
            null,
            bitmapInfo,
            WinGDI.DIB_RGB_COLORS,
            pixelPointer,
            null,
            0,
        ) ?: return null

        val pixelCount = image.width * image.height
        val pixels = IntArray(pixelCount)
        image.getRGB(0, 0, image.width, image.height, pixels, 0, image.width)
        val buffer = pixelPointer.value
            .getByteBuffer(0L, pixelCount.toLong() * 4L)
            .order(ByteOrder.LITTLE_ENDIAN)
            .asIntBuffer()
        pixels.forEach { pixel ->
            buffer.put(toPremultipliedArgb(pixel))
        }
        buffer.flip()
        return bitmap
    }

    private fun toPremultipliedArgb(pixel: Int): Int {
        val alpha = pixel ushr 24 and 0xFF
        val red = pixel shr 16 and 0xFF
        val green = pixel shr 8 and 0xFF
        val blue = pixel and 0xFF
        val premultipliedRed = (red * alpha + 127) / 255
        val premultipliedGreen = (green * alpha + 127) / 255
        val premultipliedBlue = (blue * alpha + 127) / 255
        return (alpha shl 24) or (premultipliedRed shl 16) or (premultipliedGreen shl 8) or premultipliedBlue
    }

    private fun loadRemoteImage(url: String): BufferedImage? = runCatching {
        val connection = URL(url).openConnection().apply {
            connectTimeout = 4_000
            readTimeout = 4_000
            setRequestProperty("User-Agent", "Radikall/1.0")
        }
        connection.getInputStream().use { stream ->
            ImageIO.read(stream)?.toArgbBufferedImage()
        }
    }.getOrNull()

    private fun loadBundledImage(path: String): BufferedImage? {
        val resourceStream = WindowsTaskbarPreviewController::class.java.getResourceAsStream(path) ?: return null
        return resourceStream.use { stream ->
            ImageIO.read(stream)?.toArgbBufferedImage()
        }
    }

    private fun createFallbackPreviewImage(): BufferedImage {
        val image = BufferedImage(224, 100, BufferedImage.TYPE_INT_ARGB)
        val graphics = image.createGraphics()
        graphics.color = Color(0xD0, 0x10, 0x4C)
        graphics.fillRoundRect(0, 0, image.width, image.height, 28, 28)
        graphics.color = Color.WHITE
        graphics.font = graphics.font.deriveFont(34f)
        graphics.drawString("radikall", 28, 61)
        graphics.dispose()
        return image
    }

    private fun drawThumbButtonIcon(type: IconType): BufferedImage {
        val size = 32
        val image = BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB)
        val graphics = image.createGraphics()
        graphics.color = Color.WHITE

        when (type) {
            IconType.PLAY -> {
                val triangle = Path2D.Float().apply {
                    moveTo(11.0, 8.0)
                    lineTo(25.0, 16.0)
                    lineTo(11.0, 24.0)
                    closePath()
                }
                graphics.fill(triangle)
            }

            IconType.PAUSE -> {
                graphics.fillRoundRect(9, 8, 5, 16, 4, 4)
                graphics.fillRoundRect(18, 8, 5, 16, 4, 4)
            }

            IconType.PREVIOUS -> {
                graphics.fillRect(7, 8, 3, 16)
                val triangle1 = Path2D.Float().apply {
                    moveTo(22.0, 8.0)
                    lineTo(12.0, 16.0)
                    lineTo(22.0, 24.0)
                    closePath()
                }
                val triangle2 = Path2D.Float().apply {
                    moveTo(16.0, 8.0)
                    lineTo(6.0, 16.0)
                    lineTo(16.0, 24.0)
                    closePath()
                }
                graphics.fill(triangle1)
                graphics.fill(triangle2)
            }

            IconType.NEXT -> {
                graphics.fillRect(22, 8, 3, 16)
                val triangle1 = Path2D.Float().apply {
                    moveTo(10.0, 8.0)
                    lineTo(20.0, 16.0)
                    lineTo(10.0, 24.0)
                    closePath()
                }
                val triangle2 = Path2D.Float().apply {
                    moveTo(16.0, 8.0)
                    lineTo(26.0, 16.0)
                    lineTo(16.0, 24.0)
                    closePath()
                }
                graphics.fill(triangle1)
                graphics.fill(triangle2)
            }
        }

        graphics.dispose()
        return image
    }

    private fun writeIco(target: File, image: BufferedImage) {
        val width = image.width
        val height = image.height
        val maskRowSize = ((width + 31) / 32) * 4
        val maskSize = maskRowSize * height
        val xorSize = width * height * 4
        val imageSize = 40 + xorSize + maskSize

        ByteArrayOutputStream().use { byteStream ->
            DataOutputStream(byteStream).use { output ->
                writeLeShort(output, 0)
                writeLeShort(output, 1)
                writeLeShort(output, 1)

                output.writeByte(if (width >= 256) 0 else width)
                output.writeByte(if (height >= 256) 0 else height)
                output.writeByte(0)
                output.writeByte(0)
                writeLeShort(output, 1)
                writeLeShort(output, 32)
                writeLeInt(output, imageSize)
                writeLeInt(output, 22)

                writeLeInt(output, 40)
                writeLeInt(output, width)
                writeLeInt(output, height * 2)
                writeLeShort(output, 1)
                writeLeShort(output, 32)
                writeLeInt(output, 0)
                writeLeInt(output, xorSize)
                writeLeInt(output, 0)
                writeLeInt(output, 0)
                writeLeInt(output, 0)
                writeLeInt(output, 0)

                for (y in height - 1 downTo 0) {
                    for (x in 0 until width) {
                        val pixel = image.getRGB(x, y)
                        output.writeByte(pixel and 0xFF)
                        output.writeByte(pixel shr 8 and 0xFF)
                        output.writeByte(pixel shr 16 and 0xFF)
                        output.writeByte(pixel ushr 24 and 0xFF)
                    }
                }

                repeat(maskSize) {
                    output.writeByte(0)
                }
            }
            target.writeBytes(byteStream.toByteArray())
        }
    }

    private fun runOnEdt(action: () -> Unit) {
        if (EventQueue.isDispatchThread()) {
            action()
        } else {
            EventQueue.invokeLater(action)
        }
    }

    private fun THUMBBUTTON.setTip(text: String) {
        szTip.fill('\u0000')
        text.take(szTip.size - 1).forEachIndexed { index, char ->
            szTip[index] = char
        }
    }

    private fun BufferedImage.toArgbBufferedImage(): BufferedImage {
        if (type == BufferedImage.TYPE_INT_ARGB) {
            return this
        }
        val converted = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
        val graphics = converted.createGraphics()
        graphics.drawImage(this, 0, 0, null)
        graphics.dispose()
        return converted
    }

    private fun extractLowWord(value: Long): Int = (value and 0xFFFF).toInt()

    private fun extractHighWord(value: Long): Int = ((value shr 16) and 0xFFFF).toInt()

    private enum class IconType {
        PREVIOUS,
        PLAY,
        PAUSE,
        NEXT,
    }

    private class TaskbarList3(pointer: Pointer) : Unknown(pointer) {
        fun hrInit(): HRESULT =
            _invokeNativeObject(3, arrayOf(pointer), HRESULT::class.java) as HRESULT

        fun thumbBarAddButtons(hwnd: HWND, buttons: Array<THUMBBUTTON>): HRESULT =
            _invokeNativeObject(15, arrayOf(pointer, hwnd, buttons.size, buttons[0]), HRESULT::class.java) as HRESULT

        fun thumbBarUpdateButtons(hwnd: HWND, buttons: Array<THUMBBUTTON>): HRESULT =
            _invokeNativeObject(16, arrayOf(pointer, hwnd, buttons.size, buttons[0]), HRESULT::class.java) as HRESULT

        fun setThumbnailTooltip(hwnd: HWND, text: String): HRESULT =
            _invokeNativeObject(19, arrayOf(pointer, hwnd, text), HRESULT::class.java) as HRESULT
    }

    @Structure.FieldOrder("dwMask", "iId", "iBitmap", "hIcon", "szTip", "dwFlags")
    class THUMBBUTTON : Structure() {
        @JvmField
        var dwMask: Int = 0

        @JvmField
        var iId: Int = 0

        @JvmField
        var iBitmap: Int = 0

        @JvmField
        var hIcon: HICON? = null

        @JvmField
        var szTip: CharArray = CharArray(260)

        @JvmField
        var dwFlags: Int = 0
    }

    private interface DwmApi : com.sun.jna.win32.StdCallLibrary {
        fun DwmSetWindowAttribute(hwnd: HWND, attribute: Int, value: Pointer, size: Int): HRESULT
        fun DwmInvalidateIconicBitmaps(hwnd: HWND): HRESULT
        fun DwmSetIconicThumbnail(hwnd: HWND, bitmap: HBITMAP, flags: Int): HRESULT
        companion object {
            val INSTANCE: DwmApi = Native.load("dwmapi", DwmApi::class.java)
        }
    }

    private companion object {
        val CLSID_TASKBAR_LIST = Guid.CLSID("{56FDF344-FD6D-11d0-958A-006097C9A090}")
        val IID_TASKBAR_LIST3 = Guid.IID("{EA1AFB91-9E28-4B86-90E9-9E9F8A5EEFAF}")

        const val CLSCTX_INPROC_SERVER = 0x1

        const val GWL_WNDPROC = -4
        const val WM_COMMAND = 0x0111
        const val WM_DWMSENDICONICTHUMBNAIL = 0x0323
        const val THBN_CLICKED = 0x1800

        const val BUTTON_PREVIOUS = 2001
        const val BUTTON_PLAY_PAUSE = 2002
        const val BUTTON_NEXT = 2003

        const val THB_ICON = 0x2
        const val THB_TOOLTIP = 0x4
        const val THB_FLAGS = 0x8
        const val THBF_ENABLED = 0x0
        const val THBF_DISABLED = 0x1

        const val IMAGE_ICON = 1
        const val LR_LOADFROMFILE = 0x0010

        const val DWMWA_FORCE_ICONIC_REPRESENTATION = 7
        const val DWMWA_HAS_ICONIC_BITMAP = 10

        val taskbarButtonCreatedMessage = User32.INSTANCE.RegisterWindowMessage("TaskbarButtonCreated")

        fun writeLeShort(output: DataOutputStream, value: Int) {
            output.writeByte(value and 0xFF)
            output.writeByte(value ushr 8 and 0xFF)
        }

        fun writeLeInt(output: DataOutputStream, value: Int) {
            output.writeByte(value and 0xFF)
            output.writeByte(value ushr 8 and 0xFF)
            output.writeByte(value ushr 16 and 0xFF)
            output.writeByte(value ushr 24 and 0xFF)
        }
    }
}
