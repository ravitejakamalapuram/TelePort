import sys

def patch_file(filepath):
    with open(filepath, 'r') as f:
        content = f.read()

    # Apply replacement
    content = content.replace("""/* Action Rows */
.action-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}""", """/* Action Rows */
.action-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

label.action-row {
  cursor: pointer;
}
label.action-row:hover {
  background-color: rgba(255, 255, 255, 0.05);
  border-radius: 8px;
}""")

    content = content.replace(""".slider {
  position: absolute;
  cursor: pointer;
  top: 0;""", """.slider {
  position: absolute;
  top: 0;""")

    with open(filepath, 'w') as f:
        f.write(content)

    print("Patched successfully")

if __name__ == '__main__':
    patch_file('chrome-extension/popup/popup.css')
