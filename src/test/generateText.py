import os

# Define the text you want to duplicate
text_to_duplicate = "I am the hunter\n"  # Add more text to increase file size

# Calculate the number of times the text needs to be duplicated
target_size_mb = 250
text_size_mb = len(text_to_duplicate.encode('utf-8')) / 1024 / 1024  # Convert bytes to MB
num_duplicates = int((target_size_mb / text_size_mb) + 1)

# Generate the duplicated text
duplicated_text = text_to_duplicate * num_duplicates

# Write the duplicated text to a file
file_path = "duplicated_text.txt"
with open(file_path, "w") as file:
    file.write(duplicated_text)

# Check the size of the file
file_size_mb = os.path.getsize(file_path) / 1024 / 1024
print("File created with size:", file_size_mb, "MB")
