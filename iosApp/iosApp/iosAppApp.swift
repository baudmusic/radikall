import SwiftUI
import Shared
import UserNotifications
import UIKit

final class AppDelegate: NSObject, UIApplicationDelegate, UNUserNotificationCenterDelegate {
    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey : Any]? = nil
    ) -> Bool {
        UNUserNotificationCenter.current().delegate = self
        return true
    }

    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        willPresent notification: UNNotification,
        withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void
    ) {
        let stationId = notification.request.content.userInfo["stationId"] as? String
        IOSAlarmBridge.shared.handleForegroundAlarmNotification(stationId: stationId)
        completionHandler([.banner, .list, .sound])
    }

    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        didReceive response: UNNotificationResponse,
        withCompletionHandler completionHandler: @escaping () -> Void
    ) {
        let stationId = response.notification.request.content.userInfo["stationId"] as? String
        IOSAlarmBridge.shared.handleNotificationTap(stationId: stationId)
        completionHandler()
    }
}

@main
struct iosAppApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) private var appDelegate
    @Environment(\.scenePhase) private var scenePhase

    var body: some Scene {
        WindowGroup {
            ContentView()
                .onAppear {
                    IOSAlarmBridge.shared.updateAppForeground(isForeground: true)
                }
                .onChange(of: scenePhase) { newPhase in
                    IOSAlarmBridge.shared.updateAppForeground(isForeground: newPhase != .background)
                    if newPhase == .background {
                        RadikallAppLifecycle.shared.onAppEnteredBackground()
                    }
                }
        }
    }
}
