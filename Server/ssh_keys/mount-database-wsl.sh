sudo mkdir /mnt/x
sudo sshfs -o allow_other,default_permissions,IdentityFile=/mnt/c/Users/Pierr/.ssh/ssh-key.key ubuntu@134.98.156.24:/opt/bluebridge/data /mnt/x