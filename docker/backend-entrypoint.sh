#!/bin/sh
set -e
# Том uploads монтируется поверх слоя образа и обычно root:root — без chown spring
# не может создать /data/uploads/training-library (см. TrainingLibraryServiceImpl @PostConstruct).
chown -R spring:spring /data/uploads 2>/dev/null || true
exec su-exec spring sh -c "exec java $JAVA_OPTS -jar /app/app.jar"
