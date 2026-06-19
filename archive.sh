#!/usr/bin/env bash 

zip -r archive_run21ai.zip . -x ".idea/*" ".git/*" ".gradle/*" "build/*" "*/.structurizr/*" "*/build/*" "*/node_modules/*" "*.class" "*.jar" "*.zip"
