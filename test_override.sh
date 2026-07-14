#!/bin/bash
cat app/src/main/java/com/teleport/app/tv/browser/TabManager.kt | grep -n "override fun shouldOverrideUrlLoading"
