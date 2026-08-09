import sys

def patch_file(filepath):
    with open(filepath, 'r') as f:
        content = f.read()

    # Apply replacement
    content = content.replace(""".slider {
  position: absolute;
  cursor: pointer;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;""", """.slider {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;""")

    with open(filepath, 'w') as f:
        f.write(content)

    print("Patched successfully")

if __name__ == '__main__':
    patch_file('chrome-extension/popup/popup.css')
