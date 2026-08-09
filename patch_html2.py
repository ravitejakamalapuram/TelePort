import sys

def patch_file(filepath):
    with open(filepath, 'r') as f:
        content = f.read()

    # Apply replacement 1
    content = content.replace("""<label class="action-row" for="castToggle" style="cursor: pointer;">""", """<label class="action-row" for="castToggle">""")

    # Apply replacement 2
    content = content.replace("""<label class="action-row" for="darkModeToggle" style="cursor: pointer;">""", """<label class="action-row" for="darkModeToggle">""")

    with open(filepath, 'w') as f:
        f.write(content)

    print("Patched successfully")

if __name__ == '__main__':
    patch_file('chrome-extension/popup/popup.html')
