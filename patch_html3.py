import sys

def patch_file(filepath):
    with open(filepath, 'r') as f:
        content = f.read()

    # Apply replacement 1
    content = content.replace("""<div class="switch">""", """<div class="switch">""") # noop

    with open(filepath, 'w') as f:
        f.write(content)

    print("Patched successfully")

if __name__ == '__main__':
    patch_file('chrome-extension/popup/popup.html')
