package com.radiko.desktop

import com.radiko.i18n.AppLocalizer
import com.radiko.settings.AppLanguage
import com.radiko.ui.viewmodel.PlayerState
import java.awt.AWTException
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Cursor
import java.awt.Dimension
import java.awt.EventQueue
import java.awt.FlowLayout
import java.awt.GradientPaint
import java.awt.Image
import java.awt.Insets
import java.awt.MouseInfo
import java.awt.RenderingHints
import java.awt.SystemTray
import java.awt.Toolkit
import java.awt.TrayIcon
import java.awt.Window
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.WindowEvent
import java.awt.event.WindowFocusListener
import java.awt.geom.RoundRectangle2D
import java.awt.image.BufferedImage
import javax.imageio.ImageIO
import javax.swing.BorderFactory
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JTextArea
import javax.swing.JWindow

private val TrayAccentBlue = Color(0x00, 0x5C, 0xAF)
private val TrayAccentBlueDark = Color(0x11, 0x32, 0x85)
private val TrayAccentRed = Color(0xD0, 0x10, 0x4C)
private val TrayPanelBackground = Color(0xEA, 0xF3, 0xF7)
private val TrayPanelBackgroundAlt = Color(0xF7, 0xFB, 0xFC)
private val TrayPanelBorder = Color(0xC7, 0xDD, 0xE8)
private val TrayTextPrimary = Color(0x2C, 0x2C, 0x2A)
private val TrayTextSecondary = Color(0x6B, 0x69, 0x66)

class DesktopTrayController(
    private val onRestoreWindow: () -> Unit,
    private val onChooseStation: () -> Unit,
    private val onTogglePlayback: () -> Unit,
    private val onExit: () -> Unit,
) {
    private var trayIcon: TrayIcon? = null
    private var popupWindow: JWindow? = null
    private var popupVisible = false
    private var currentState: PlayerState = PlayerState()
    private var lastPopupToggleAt: Long = 0L

    // 直接持有 AWT window 引用，不依赖 Compose state 传播来做 toFront()
    private var attachedWindow: Window? = null

    private var currentLanguage: AppLanguage = AppLanguage.ENGLISH
    private var trayTitleLabel: JTextArea? = null
    private var stationLabel: JLabel? = null
    private var statusLabel: JLabel? = null
    private var programText: JTextArea? = null
    private var chooseButton: JButton? = null
    private var toggleButton: JButton? = null
    private var exitButton: JButton? = null

    fun isSupported(): Boolean = SystemTray.isSupported()

    /**
     * 直接通过 AWT 窗口引用置前，同时通知 Compose 同步 isWindowVisible 状态。
     * 不走 Compose state → LaunchedEffect 这条链，因为当窗口已可见时 state 不会重新触发。
     */
    private fun showMainWindow() {
        onRestoreWindow() // 通知 Compose 同步状态
        val w = attachedWindow ?: return
        w.isVisible = true
        (w as? java.awt.Frame)?.extendedState = java.awt.Frame.NORMAL
        w.isAlwaysOnTop = true
        w.toFront()
        w.requestFocus()
        w.isAlwaysOnTop = false
    }

    fun attach(window: Window) {
        if (!isSupported() || trayIcon != null) {
            return
        }
        attachedWindow = window

        val icon = TrayIcon(loadTrayImage(), "Radikall").apply {
            isImageAutoSize = true
            // 双击也唤起窗口
            addActionListener {
                hidePopup()
                EventQueue.invokeLater { showMainWindow() }
            }
            addMouseListener(object : MouseAdapter() {
                override fun mouseClicked(event: MouseEvent) {
                    if (event.button == MouseEvent.BUTTON1 && !event.isPopupTrigger) {
                        // 左键单击：唤起主窗口
                        EventQueue.invokeLater { showMainWindow() }
                    } else if (event.button == MouseEvent.BUTTON3 || event.isPopupTrigger) {
                        // 右键：切换托盘弹窗
                        maybeTogglePopup()
                    }
                }
            })
        }

        runCatching {
            SystemTray.getSystemTray().add(icon)
            trayIcon = icon
            update(currentState)
        }.onFailure { throwable ->
            val message = if (throwable is AWTException) throwable.message else throwable.toString()
            println("Radikall tray: failed to add tray icon: $message")
        }
    }

    fun update(
        state: PlayerState,
        language: AppLanguage = currentLanguage,
    ) {
        currentState = state
        currentLanguage = language
        val strings = AppLocalizer.strings(language)
        val station = state.currentStation?.name ?: "Radikall"
        val status = if (state.isPlaying) strings.nowPlayingStatus else strings.currentlyNotPlaying
        val program = state.currentProgram?.title?.takeIf { it.isNotBlank() }
            ?: strings.noProgramPlaying

        trayIcon?.toolTip = "$station\n${truncate(program, 64)}"

        EventQueue.invokeLater {
            trayTitleLabel?.text = strings.trayPlaybackControl
            stationLabel?.text = station
            statusLabel?.text = status
            programText?.text = program
            chooseButton?.text = strings.chooseStation
            toggleButton?.text = if (state.isPlaying) strings.pause else strings.play
            exitButton?.text = strings.exitApp
        }
    }

    fun hidePopup() {
        popupVisible = false
        EventQueue.invokeLater {
            popupWindow?.isVisible = false
        }
    }

    fun dispose() {
        hidePopup()
        EventQueue.invokeLater {
            popupWindow?.dispose()
            popupWindow = null
        }
        trayIcon?.let { icon ->
            runCatching {
                SystemTray.getSystemTray().remove(icon)
            }
        }
        trayIcon = null
        attachedWindow = null
    }

    private fun maybeTogglePopup() {
        val now = System.currentTimeMillis()
        if (now - lastPopupToggleAt < 150L) {
            return
        }
        lastPopupToggleAt = now

        EventQueue.invokeLater {
            if (popupVisible) {
                hidePopup()
            } else {
                // TrayIcon 的 event.x/y 在 Windows 上是相对托盘图标的本地坐标，
                // 用 MouseInfo 获取真正的屏幕坐标。
                val screenPos = MouseInfo.getPointerInfo().location
                showPopup(screenPos.x, screenPos.y)
            }
        }
    }

    private fun showPopup(anchorX: Int, anchorY: Int) {
        val popup = ensurePopupWindow()
        popup.pack()
        val screenSize = Toolkit.getDefaultToolkit().screenSize
        val x = (anchorX - popup.width + 16).coerceIn(0, screenSize.width - popup.width)
        val y = (anchorY - popup.height - 16).coerceIn(0, screenSize.height - popup.height)
        popup.setLocation(x, y)
        popupVisible = true
        popup.isVisible = true
        popup.toFront()
        popup.requestFocus()
    }

    private fun ensurePopupWindow(): JWindow {
        popupWindow?.let { return it }
        val strings = AppLocalizer.strings(currentLanguage)

        val popup = JWindow().apply {
            background = Color(0, 0, 0, 0)
            contentPane.background = Color(0, 0, 0, 0)
            isAlwaysOnTop = true
            addWindowFocusListener(object : WindowFocusListener {
                override fun windowGainedFocus(e: WindowEvent) {}
                override fun windowLostFocus(e: WindowEvent) {
                    hidePopup()
                }
            })
        }

        val card = RoundedPanel().apply {
            layout = BorderLayout()
            background = TrayPanelBackground
            border = BorderFactory.createEmptyBorder(20, 22, 18, 22)
            preferredSize = Dimension(560, 252)
        }

        val header = JPanel(BorderLayout(14, 0)).apply {
            isOpaque = false
        }
        val titleColumn = JPanel().apply {
            isOpaque = false
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
        }
        trayTitleLabel = createHeaderText(
            strings.trayPlaybackControl,
            15f,
            TrayAccentBlue,
            true,
        ).apply {
            preferredSize = Dimension(420, 24)
            minimumSize = Dimension(420, 24)
            maximumSize = Dimension(420, 24)
        }
        titleColumn.add(trayTitleLabel!!)
        titleColumn.add(Box.createVerticalStrut(6))
        stationLabel = createLabel(strings.currentlyNotPlaying, 28f, TrayTextPrimary, true)
        titleColumn.add(stationLabel!!)
        titleColumn.border = BorderFactory.createEmptyBorder(0, 0, 0, 12)
        header.add(titleColumn, BorderLayout.CENTER)
        header.add(createFlatButton("\u00d7", TrayTextSecondary) {
            hidePopup()
        }, BorderLayout.EAST)

        val body = JPanel().apply {
            isOpaque = false
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            border = BorderFactory.createEmptyBorder(18, 0, 0, 0)
        }
        statusLabel = createLabel(strings.currentlyNotPlaying, 14f, TrayTextSecondary, true)
        body.add(statusLabel!!)
        body.add(Box.createVerticalStrut(10))
        programText = JTextArea(strings.noProgramPlaying).apply {
            isOpaque = false
            isEditable = false
            lineWrap = true
            wrapStyleWord = true
            font = font.deriveFont(java.awt.Font.BOLD, 17f)
            foreground = TrayTextPrimary
            border = BorderFactory.createEmptyBorder()
            alignmentX = 0f
            rows = 3
            margin = Insets(0, 0, 0, 0)
            highlighter = null
        }
        body.add(programText!!)
        body.add(Box.createVerticalGlue())

        // 用 BoxLayout 替代 FlowLayout，确保三个按钮在固定宽度内平均分配
        val actionRow = JPanel().apply {
            isOpaque = false
            layout = BoxLayout(this, BoxLayout.X_AXIS)
            border = BorderFactory.createEmptyBorder(18, 0, 0, 0)
        }
        chooseButton = createActionButton(strings.chooseStation, false) {
            hidePopup()
            EventQueue.invokeLater {
                onChooseStation() // 通知 Compose 隐藏 NowPlaying 并同步状态
                showMainWindow()
            }
        }
        toggleButton = createActionButton(
            if (currentState.isPlaying) strings.pause else strings.play, true
        ) {
            hidePopup()
            EventQueue.invokeLater {
                onTogglePlayback()
            }
        }
        exitButton = createActionButton(strings.exitApp, false) {
            hidePopup()
            EventQueue.invokeLater {
                onExit()
            }
        }
        actionRow.add(chooseButton!!)
        actionRow.add(Box.createHorizontalStrut(12))
        actionRow.add(toggleButton!!)
        actionRow.add(Box.createHorizontalStrut(12))
        actionRow.add(exitButton!!)

        card.add(header, BorderLayout.NORTH)
        card.add(body, BorderLayout.CENTER)
        card.add(actionRow, BorderLayout.SOUTH)
        popup.contentPane.add(card)

        // 弹窗创建时立即同步当前播放状态，避免首次打开显示过时的默认文字
        stationLabel!!.text = currentState.currentStation?.name ?: strings.currentlyNotPlaying
        statusLabel!!.text = if (currentState.isPlaying) strings.nowPlayingStatus else strings.currentlyNotPlaying
        programText!!.text = currentState.currentProgram?.title?.takeIf { it.isNotBlank() }
            ?: strings.noProgramPlaying

        popupWindow = popup
        return popup
    }

    private fun createLabel(
        text: String,
        size: Float,
        color: Color,
        bold: Boolean,
    ): JLabel {
        return JLabel(text).apply {
            foreground = color
            font = font.deriveFont(if (bold) java.awt.Font.BOLD else java.awt.Font.PLAIN, size)
            alignmentX = 0f
        }
    }

    private fun createHeaderText(
        text: String,
        size: Float,
        color: Color,
        bold: Boolean,
    ): JTextArea {
        return JTextArea(text).apply {
            isOpaque = false
            isEditable = false
            lineWrap = false
            wrapStyleWord = false
            foreground = color
            font = font.deriveFont(if (bold) java.awt.Font.BOLD else java.awt.Font.PLAIN, size)
            border = BorderFactory.createEmptyBorder()
            margin = Insets(0, 0, 0, 0)
            highlighter = null
            rows = 1
            columns = 18
            alignmentX = 0f
        }
    }

    private fun createFlatButton(text: String, color: Color, onClick: () -> Unit): JButton {
        return JButton(text).apply {
            isOpaque = false
            isContentAreaFilled = false
            isBorderPainted = false
            isFocusPainted = false
            border = BorderFactory.createEmptyBorder(6, 8, 6, 8)
            foreground = color
            cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
            addActionListener { onClick() }
        }
    }

    private fun createActionButton(
        text: String,
        primary: Boolean,
        onClick: () -> Unit,
    ): JButton {
        return RoundedButton(
            backgroundColor = if (primary) TrayAccentBlue else TrayPanelBackgroundAlt,
            backgroundColorEnd = if (primary) TrayAccentBlueDark else TrayPanelBackgroundAlt,
            borderColor = if (primary) TrayAccentBlue else TrayPanelBorder,
            textColor = if (primary) Color.WHITE else TrayAccentBlue,
        ).apply {
            this.text = text
            // preferredSize 宽度设 0，让 BoxLayout 平均分配可用宽度
            preferredSize = Dimension(0, 52)
            minimumSize = Dimension(60, 52)
            maximumSize = Dimension(Int.MAX_VALUE, 52)
            margin = Insets(0, 8, 0, 8)
            isFocusPainted = false
            cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
            font = font.deriveFont(java.awt.Font.BOLD, 18f)
            addActionListener { onClick() }
        }
    }

    private fun truncate(text: String, maxLength: Int): String {
        if (text.length <= maxLength) {
            return text
        }
        return text.take(maxLength - 1) + "\u2026"
    }

    private fun loadTrayImage(): Image {
        val resourceStream = DesktopTrayController::class.java.getResourceAsStream("/logo2.png")
        if (resourceStream != null) {
            resourceStream.use { stream ->
                ImageIO.read(stream)?.let { return it }
            }
        }

        val fallback = BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB)
        val graphics = fallback.createGraphics()
        graphics.color = TrayAccentRed
        graphics.fillRoundRect(0, 0, 64, 64, 18, 18)
        graphics.color = Color.WHITE
        graphics.font = graphics.font.deriveFont(28f)
        graphics.drawString("R", 20, 43)
        graphics.dispose()
        return fallback
    }
}

private class RoundedPanel : JPanel() {
    init {
        isOpaque = false
    }

    override fun paintComponent(graphics: java.awt.Graphics) {
        val g2 = graphics.create() as java.awt.Graphics2D
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g2.color = background
        g2.fill(RoundRectangle2D.Float(0f, 0f, width.toFloat(), height.toFloat(), 30f, 30f))
        g2.color = TrayPanelBorder
        g2.draw(RoundRectangle2D.Float(0.5f, 0.5f, width - 1f, height - 1f, 30f, 30f))
        g2.dispose()
        super.paintComponent(graphics)
    }
}

private class RoundedButton(
    private val backgroundColor: Color,
    private val backgroundColorEnd: Color,
    private val borderColor: Color,
    private val textColor: Color,
) : JButton() {
    init {
        isContentAreaFilled = false
        isBorderPainted = false
        isOpaque = false
        foreground = textColor
    }

    override fun paintComponent(graphics: java.awt.Graphics) {
        val g2 = graphics.create() as java.awt.Graphics2D
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g2.paint = GradientPaint(
            0f,
            0f,
            backgroundColor,
            width.toFloat(),
            height.toFloat(),
            backgroundColorEnd,
        )
        g2.fill(RoundRectangle2D.Float(0f, 0f, width.toFloat(), height.toFloat(), 24f, 24f))
        g2.color = borderColor
        g2.draw(RoundRectangle2D.Float(0.5f, 0.5f, width - 1f, height - 1f, 24f, 24f))
        g2.dispose()
        super.paintComponent(graphics)
    }
}
