get-android-dumps:
	@set -e; \
	rm -f /tmp/android-sync-dumps.tar; \
	rm -rf /tmp/android-sync-dumps; \
	echo "Pulling dumps via adb exec-out..."; \
	adb exec-out 'run-as com.romankozak.forwardappmobile.debug tar -cf - -C /data/user/0/com.romankozak.forwardappmobile.debug/files sync-dumps' > /tmp/android-sync-dumps.tar; \
	mkdir -p /tmp/android-sync-dumps; \
	if tar -tf /tmp/android-sync-dumps.tar >/dev/null 2>&1; then \
		tar -xf /tmp/android-sync-dumps.tar -C /tmp/android-sync-dumps; \
		echo "Android dumps extracted to /tmp/android-sync-dumps"; \
	else \
		echo "Failed to extract dumps: tar stream invalid (maybe empty or permission issue)"; \
	fi
