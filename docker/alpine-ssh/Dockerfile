FROM alpine:3.19

# Install OpenSSH server + metric tools
RUN apk add --no-cache openssh-server procps coreutils

# Create SSH user
RUN adduser -D -s /bin/sh sshuser \
    && echo "sshuser:sshpass" | chpasswd

# Generate host keys and configure SSH
RUN ssh-keygen -A \
    && sed -i 's/#PasswordAuthentication yes/PasswordAuthentication yes/' /etc/ssh/sshd_config \
    && sed -i 's/#PermitRootLogin prohibit-password/PermitRootLogin no/' /etc/ssh/sshd_config

EXPOSE 22

CMD ["/usr/sbin/sshd", "-D", "-e"]