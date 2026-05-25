#!/usr/bin/env python3
# ============================================================================
# compile_branding.py — Unifies and propagates design tokens & metadata.
# ============================================================================
import os
import json
import re

def to_compose_color(hex_str):
    # Convert hex (e.g. #7928ca) to Jetpack Compose Color(0xFF7928CA)
    clean = hex_str.replace('#', '').upper()
    return f"Color(0xFF{clean})"

def main():
    script_dir = os.path.dirname(os.path.abspath(__file__))
    project_root = os.path.dirname(script_dir)
    
    config_path = os.path.join(project_root, "teleport-config.json")
    if not os.path.exists(config_path):
        print(f"Error: Configuration file not found at {config_path}")
        return
        
    print(f"Reading branding configuration from: {config_path}")
    with open(config_path, 'r') as f:
        config = json.load(f)
        
    colors = config.get("colors", {})
    support_email = config.get("supportEmail", "")
    app_name = config.get("appName", "TelePort")
    port = config.get("port", 8080)
    
    # ------------------------------------------------------------------------
    # 1. Compile CSS Variable theme tokens
    # ------------------------------------------------------------------------
    css_content = f"""/* Generated automatically from teleport-config.json. Do not edit directly. */
:root {{
  --bg-color: {colors.get("background", "#0d0d11")};
  --card-bg: {colors.get("cardBg", "rgba(26, 26, 36, 0.55)")};
  --border-color: {colors.get("border", "rgba(255, 255, 255, 0.08)")};
  --primary-color: {colors.get("primary", "#7928ca")};
  --accent-color: {colors.get("accent", "#00dfd8")};
  --success-color: {colors.get("success", "#02c39a")};
  --error-color: {colors.get("error", "#ff3b30")};
  --text-primary: {colors.get("textMain", "#ffffff")};
  --text-secondary: {colors.get("textSub", "#9ea2b0")};
  --support-email: "{support_email}";
}}
"""

    # Write CSS files
    chrome_css_path = os.path.join(project_root, "chrome-extension", "popup", "theme-tokens.css")
    os.makedirs(os.path.dirname(chrome_css_path), exist_ok=True)
    with open(chrome_css_path, 'w') as f:
        f.write(css_content)
    print(f"Compiled Chrome CSS tokens to: {chrome_css_path}")

    docs_css_path = os.path.join(project_root, "docs", "theme-tokens.css")
    os.makedirs(os.path.dirname(docs_css_path), exist_ok=True)
    with open(docs_css_path, 'w') as f:
        f.write(css_content)
    print(f"Compiled Docs CSS tokens to: {docs_css_path}")

    # ------------------------------------------------------------------------
    # 2. Compile Android Jetpack Compose ThemeTokens.kt
    # ------------------------------------------------------------------------
    theme_tokens_dir = os.path.join(project_root, "app", "src", "main", "java", "com", "teleport", "app", "ui", "theme")
    os.makedirs(theme_tokens_dir, exist_ok=True)
    theme_tokens_path = os.path.join(theme_tokens_dir, "ThemeTokens.kt")
    
    kotlin_content = f"""package com.teleport.app.ui.theme

import androidx.compose.ui.graphics.Color

// Generated automatically from teleport-config.json. Do not edit directly.
object ThemeTokens {{
    val Background = {to_compose_color(colors.get("background", "#0d0d11"))}
    val Primary = {to_compose_color(colors.get("primary", "#7928ca"))}
    val Accent = {to_compose_color(colors.get("accent", "#00dfd8"))}
    val Success = {to_compose_color(colors.get("success", "#02c39a"))}
    val Error = {to_compose_color(colors.get("error", "#ff3b30"))}
    val CardBg = {to_compose_color(colors.get("cardBgHex", "#1a1a24"))}
    val Border = {to_compose_color(colors.get("borderHex", "#1f1f2a"))}
    val TextMain = {to_compose_color(colors.get("textMain", "#ffffff"))}
    val TextSub = {to_compose_color(colors.get("textSub", "#9ea2b0"))}
    const val SUPPORT_EMAIL = "{support_email}"
    const val APP_NAME = "{app_name}"
    const val PORT = {port}
}}
"""
    with open(theme_tokens_path, 'w') as f:
        f.write(kotlin_content)
    print(f"Compiled Kotlin ThemeTokens to: {theme_tokens_path}")

    # ------------------------------------------------------------------------
    # 3. Inject CSS Variables into Ktor Server (LocalServerService.kt)
    # ------------------------------------------------------------------------
    server_service_path = os.path.join(project_root, "app", "src", "main", "java", "com", "teleport", "app", "tv", "server", "LocalServerService.kt")
    if os.path.exists(server_service_path):
        print(f"Injecting Ktor Web Remote CSS variables into: {server_service_path}")
        with open(server_service_path, 'r') as f:
            content = f.read()
            
        # Target replacement block between comment flags
        pattern = r"(/\*\s*CSS_THEME_TOKENS_START\s*\*/).*?(/\*\s*CSS_THEME_TOKENS_END\s*\*/)"
        replacement_block = f"""/* CSS_THEME_TOKENS_START */
        :root {{
            --bg-color: {colors.get("background", "#0d0d11")};
            --primary-color: {colors.get("primary", "#7928ca")};
            --accent-color: {colors.get("accent", "#00dfd8")};
            --card-bg: {colors.get("cardBgHex", "#1a1a24")};
            --border-color: {colors.get("borderHex", "#1f1f2a")};
        }}
        /* CSS_THEME_TOKENS_END */"""
        
        updated_content = re.sub(pattern, replacement_block, content, flags=re.DOTALL)
        with open(server_service_path, 'w') as f:
            f.write(updated_content)
        print("Successfully updated Ktor Web Remote CSS block.")

    # ------------------------------------------------------------------------
    # 4. Update dynamic support email address & app name in docs/privacy.html
    # ------------------------------------------------------------------------
    privacy_path = os.path.join(project_root, "docs", "privacy.html")
    if os.path.exists(privacy_path):
        print(f"Updating privacy.html support email and appName in: {privacy_path}")
        with open(privacy_path, 'r') as f:
            content = f.read()
            
        # Find contact-box email link and replace it
        email_pattern = r"(contact us at:\s*<a href=\"mailto:)[^\"]+(\">)[^<]+(</a>)"
        replacement_email = f'\\1{support_email}\\2{support_email}\\3'
        updated_content = re.sub(email_pattern, replacement_email, content, flags=re.IGNORECASE)
        
        # Replace app name inside comments
        updated_content = re.sub(r"<!-- APP_NAME -->.*?<!-- /APP_NAME -->", f"<!-- APP_NAME -->{app_name}<!-- /APP_NAME -->", updated_content)
        
        with open(privacy_path, 'w') as f:
            f.write(updated_content)
        print("Successfully updated privacy policy.")

    # ------------------------------------------------------------------------
    # 5. Update AndroidManifest.xml deep link port
    # ------------------------------------------------------------------------
    android_manifest_path = os.path.join(project_root, "app", "src", "main", "AndroidManifest.xml")
    if os.path.exists(android_manifest_path):
        print(f"Updating deep link port in: {android_manifest_path}")
        with open(android_manifest_path, 'r') as f:
            content = f.read()
            
        # Safely match deep link data tag and replace port
        pattern = r'(android:scheme="http"\s+android:host="\*"\s+android:port=")\d+("\s+android:path="/remote")'
        replacement = f'\\g<1>{port}\\g<2>'
        updated_content = re.sub(pattern, replacement, content)
        
        with open(android_manifest_path, 'w') as f:
            f.write(updated_content)
        print("Successfully updated AndroidManifest.xml deep link port.")

    # ------------------------------------------------------------------------
    # 6. Update app name in docs/index.html
    # ------------------------------------------------------------------------
    index_path = os.path.join(project_root, "docs", "index.html")
    if os.path.exists(index_path):
        print(f"Updating app name in: {index_path}")
        with open(index_path, 'r') as f:
            content = f.read()
        content = re.sub(r"<!-- APP_NAME -->.*?<!-- /APP_NAME -->", f"<!-- APP_NAME -->{app_name}<!-- /APP_NAME -->", content)
        with open(index_path, 'w') as f:
            f.write(content)
        print("Successfully updated docs landing page.")

    # ------------------------------------------------------------------------
    # 7. Update app name in chrome-extension/popup/popup.html
    # ------------------------------------------------------------------------
    popup_html_path = os.path.join(project_root, "chrome-extension", "popup", "popup.html")
    if os.path.exists(popup_html_path):
        print(f"Updating app name in: {popup_html_path}")
        with open(popup_html_path, 'r') as f:
            content = f.read()
        content = re.sub(r"<!-- APP_NAME -->.*?<!-- /APP_NAME -->", f"<!-- APP_NAME -->{app_name}<!-- /APP_NAME -->", content)
        with open(popup_html_path, 'w') as f:
            f.write(content)
        print("Successfully updated chrome extension popup html.")

    # ------------------------------------------------------------------------
    # 8. Update app name in chrome-extension/manifest.json
    # ------------------------------------------------------------------------
    manifest_path = os.path.join(project_root, "chrome-extension", "manifest.json")
    if os.path.exists(manifest_path):
        print(f"Updating app name in: {manifest_path}")
        with open(manifest_path, 'r') as f:
            data = json.load(f)
        data["name"] = f"{app_name} Cast & Remote"
        with open(manifest_path, 'w') as f:
            json.dump(data, f, indent=2)
        print("Successfully updated chrome extension manifest name.")

    # ------------------------------------------------------------------------
    # 9. Update app name & port in chrome-extension/service-worker.js
    # ------------------------------------------------------------------------
    sw_path = os.path.join(project_root, "chrome-extension", "service-worker.js")
    if os.path.exists(sw_path):
        print(f"Updating service-worker.js: {sw_path}")
        with open(sw_path, 'r') as f:
            content = f.read()
        # Update port
        content = re.sub(r"(ws://\${tvIp}:)\d+(/control)", f"\\g<1>{port}\\g<2>", content)
        # Update context menu app name
        content = re.sub(r'("Send active page to ).*?( TV")', f'\\1{app_name}\\2', content)
        content = re.sub(r'("Mirror active tab to ).*?( TV")', f'\\1{app_name}\\2', content)
        with open(sw_path, 'w') as f:
            f.write(content)
        print("Successfully updated service-worker.js.")

    # ------------------------------------------------------------------------
    # 10. Update port in chrome-extension/popup/popup.js
    # ------------------------------------------------------------------------
    popup_js_path = os.path.join(project_root, "chrome-extension", "popup", "popup.js")
    if os.path.exists(popup_js_path):
        print(f"Updating popup.js: {popup_js_path}")
        with open(popup_js_path, 'r') as f:
            content = f.read()
        content = re.sub(r"(ws://\${tvIp}:)\d+(/control)", f"\\g<1>{port}\\g<2>", content)
        with open(popup_js_path, 'w') as f:
            f.write(content)
        print("Successfully updated popup.js.")

    # ------------------------------------------------------------------------
    # 11. Update port in chrome-extension/offscreen/offscreen.js
    # ------------------------------------------------------------------------
    offscreen_js_path = os.path.join(project_root, "chrome-extension", "offscreen", "offscreen.js")
    if os.path.exists(offscreen_js_path):
        print(f"Updating offscreen.js: {offscreen_js_path}")
        with open(offscreen_js_path, 'r') as f:
            content = f.read()
        content = re.sub(r"(ws://\${tvIp}:)\d+(/mirror)", f"\\g<1>{port}\\g<2>", content)
        with open(offscreen_js_path, 'w') as f:
            f.write(content)
        print("Successfully updated offscreen.js.")

    # ------------------------------------------------------------------------
    # 12. Update app name in Android strings.xml
    # ------------------------------------------------------------------------
    strings_xml_path = os.path.join(project_root, "app", "src", "main", "res", "values", "strings.xml")
    if os.path.exists(strings_xml_path):
        print(f"Updating Android strings.xml app name in: {strings_xml_path}")
        with open(strings_xml_path, 'r') as f:
            content = f.read()
        content = re.sub(r'(<string name="app_name">)[^<]+(</string>)', f'\\1{app_name}\\2', content)
        with open(strings_xml_path, 'w') as f:
            f.write(content)
        print("Successfully updated Android strings.xml.")

if __name__ == "__main__":
    main()

