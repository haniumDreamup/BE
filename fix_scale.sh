#!/bin/bash

# Find all Java files with scale attributes for Double fields
echo "Fixing scale attributes in entity files..."

# List of files to fix
files=(
  "src/main/java/com/bifai/reminder/bifai_backend/entity/MedicationAdherence.java"
  "src/main/java/com/bifai/reminder/bifai_backend/entity/LocationHistory.java"
  "src/main/java/com/bifai/reminder/bifai_backend/entity/CapturedImage.java"
  "src/main/java/com/bifai/reminder/bifai_backend/entity/AnalysisResult.java"
  "src/main/java/com/bifai/reminder/bifai_backend/entity/HealthMetric.java"
  "src/main/java/com/bifai/reminder/bifai_backend/entity/ReminderTemplate.java"
  "src/main/java/com/bifai/reminder/bifai_backend/entity/NotificationDelivery.java"
  "src/main/java/com/bifai/reminder/bifai_backend/entity/ConnectivityLog.java"
  "src/main/java/com/bifai/reminder/bifai_backend/entity/ActivityLog.java"
  "src/main/java/com/bifai/reminder/bifai_backend/entity/Medication.java"
)

for file in "${files[@]}"; do
  if [ -f "$file" ]; then
    echo "Processing $file..."
    # Remove scale attribute from @Column annotations with Double fields
    sed -i '' 's/, scale = [0-9]*//g' "$file"
    echo "Fixed $file"
  fi
done

echo "All files processed!"