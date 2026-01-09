import os

file_path = r"H:\Heronix\Heronix-SchedulerV2\src\main\java\com\heronix\scheduler\controller\ui\RoomsController.java"
with open(file_path, 'r', encoding='utf-8') as f:
    content = f.read()

# Fix the incomplete variable assignment
content = content.replace(
    """                    try {
                        String generatedPhone = // TODO: Method generateRoomPhoneNumber() does not exist
                    // districtSettingsService.generateRoomPhoneNumber(newVal);
                        if (generatedPhone != null && !generatedPhone.isEmpty()) {""",
    """                    try {
                        // TODO: Method generateRoomPhoneNumber() does not exist
                        String generatedPhone = null; // districtSettingsService.generateRoomPhoneNumber(newVal);
                        if (generatedPhone != null && !generatedPhone.isEmpty()) {"""
)

with open(file_path, 'w', encoding='utf-8') as f:
    f.write(content)

print("Fixed RoomsController.java line 758")
