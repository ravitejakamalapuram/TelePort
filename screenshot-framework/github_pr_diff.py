#!/usr/bin/env python3
import os
import subprocess
import sys

def get_git_info(project_root):
    try:
        # Get current branch
        branch = subprocess.check_output(
            ["git", "-C", project_root, "rev-parse", "--abbrev-ref", "HEAD"],
            text=True
        ).strip()
        
        # Get remote URL (origin)
        remote_url = subprocess.check_output(
            ["git", "-C", project_root, "remote", "get-url", "origin"],
            text=True
        ).strip()
        
        # Parse github owner/repo from remote URL
        # Handles git@github.com:owner/repo.git or https://github.com/owner/repo.git
        if "github.com" in remote_url:
            parts = remote_url.split("github.com")[-1].replace(":", "/").strip("/")
            if parts.endswith(".git"):
                parts = parts[:-4]
            return parts, branch
    except Exception:
        pass
    return None, None

def main():
    project_root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    screenshots_dir = os.path.join(project_root, "docs", "screenshots")
    diff_dir = os.path.join(screenshots_dir, "diff")
    comment_file = os.path.join(screenshots_dir, "pr_comment.md")
    
    if not os.path.exists(diff_dir) or not os.listdir(diff_dir):
        # No differences found
        content = (
            "## ✅ Visual Regression Testing Passed\n\n"
            "All headless screenshot tests match the golden baseline images. No visual regressions detected!\n"
        )
        with open(comment_file, "w") as f:
            f.write(content)
        print(f"Generated success comment file at: {comment_file}")
        return
        
    diff_images = [f for f in os.listdir(diff_dir) if f.endswith(".png")]
    diff_images.sort()
    
    repo_path, branch = get_git_info(project_root)
    
    content = [
        "## 🚫 Visual Regression Test Failure\n\n",
        f"**{len(diff_images)} visual mismatch(es)** detected against the golden baselines! ",
        "Please review the comparison tables below (shows **Golden Baseline** vs **Diff Highlight (Red)** vs **Current Branch**):\n\n"
    ]
    
    for filename in diff_images:
        content.append(f"### 📸 `{filename}`\n\n")
        
        # Build image URL
        if repo_path and branch:
            # We construct a Raw GitHub URL so it renders properly in pull requests comments
            image_url = f"https://raw.githubusercontent.com/{repo_path}/{branch}/docs/screenshots/diff/{filename}"
            content.append(f"![Visual Diff for {filename}]({image_url})\n\n")
        else:
            # Fallback to local markdown path
            content.append(f"![Visual Diff for {filename}](./docs/screenshots/diff/{filename})\n\n")
            
        content.append("---\n\n")
        
    content.append(
        "💡 *To promote the current visual changes as the new golden baselines, run:*\n"
        "```bash\n"
        "./scripts/dev.sh update-baselines\n"
        "```\n"
    )
    
    os.makedirs(os.path.dirname(comment_file), exist_ok=True)
    with open(comment_file, "w") as f:
        f.write("".join(content))
        
    print(f"Generated PR diff comment file at: {comment_file}")

if __name__ == "__main__":
    main()
