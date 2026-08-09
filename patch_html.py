import sys

def patch_file(filepath):
    with open(filepath, 'r') as f:
        content = f.read()

    # Apply replacement 1
    content = content.replace("""      <div class="action-row">
        <div class="action-info">
          <h4>Tab Cast</h4>
          <p>Mirror active tab to TV</p>
        </div>
        <label class="switch">
          <input type="checkbox" id="castToggle" aria-label="Toggle Tab Cast">
          <span class="slider"></span>
        </label>
      </div>""", """      <label class="action-row" for="castToggle">
        <div class="action-info">
          <h4>Tab Cast</h4>
          <p>Mirror active tab to TV</p>
        </div>
        <div class="switch">
          <input type="checkbox" id="castToggle" aria-label="Toggle Tab Cast">
          <span class="slider"></span>
        </div>
      </label>""")

    # Apply replacement 2
    content = content.replace("""      <div class="action-row">
        <div class="action-info">
          <h4>TV Dark Mode</h4>
          <p>Invert light page backgrounds</p>
        </div>
        <label class="switch">
          <input type="checkbox" id="darkModeToggle" aria-label="Toggle TV Dark Mode">
          <span class="slider"></span>
        </label>
      </div>""", """      <label class="action-row" for="darkModeToggle">
        <div class="action-info">
          <h4>TV Dark Mode</h4>
          <p>Invert light page backgrounds</p>
        </div>
        <div class="switch">
          <input type="checkbox" id="darkModeToggle" aria-label="Toggle TV Dark Mode">
          <span class="slider"></span>
        </div>
      </label>""")

    with open(filepath, 'w') as f:
        f.write(content)

    print("Patched successfully")

if __name__ == '__main__':
    patch_file('chrome-extension/popup/popup.html')
