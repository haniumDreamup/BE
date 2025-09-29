# Android Testing Environment with Flutter Support
FROM ubuntu:22.04

# Set non-interactive mode for apt-get
ENV DEBIAN_FRONTEND=noninteractive
ENV TZ=Asia/Seoul

# Install basic dependencies
RUN apt-get update && apt-get install -y \
    curl \
    wget \
    unzip \
    git \
    openjdk-11-jdk \
    xvfb \
    x11vnc \
    fluxbox \
    xterm \
    supervisor \
    netcat \
    tzdata \
    && ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone \
    && rm -rf /var/lib/apt/lists/*

# Set JAVA_HOME
ENV JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64

# Install Android SDK
ENV ANDROID_HOME=/opt/android-sdk
ENV PATH=${PATH}:${ANDROID_HOME}/cmdline-tools/latest/bin:${ANDROID_HOME}/platform-tools:${ANDROID_HOME}/emulator

RUN mkdir -p ${ANDROID_HOME} && \
    cd ${ANDROID_HOME} && \
    wget https://dl.google.com/android/repository/commandlinetools-linux-9477386_latest.zip && \
    unzip commandlinetools-linux-9477386_latest.zip && \
    mkdir -p cmdline-tools/latest && \
    mv cmdline-tools/* cmdline-tools/latest/ 2>/dev/null || true && \
    rm commandlinetools-linux-9477386_latest.zip

# Accept licenses and install required packages
RUN yes | sdkmanager --licenses && \
    sdkmanager "platform-tools" "platforms;android-33" "build-tools;33.0.0" \
    "system-images;android-33;google_apis;x86_64" "emulator"

# Install Flutter
ENV FLUTTER_HOME=/opt/flutter
ENV PATH=${PATH}:${FLUTTER_HOME}/bin

RUN cd /opt && \
    wget https://storage.googleapis.com/flutter_infra_release/releases/stable/linux/flutter_linux_3.16.0-stable.tar.xz && \
    tar xf flutter_linux_3.16.0-stable.tar.xz && \
    rm flutter_linux_3.16.0-stable.tar.xz

# Create AVD
RUN echo "no" | avdmanager create avd \
    --force \
    --name test_android \
    --abi google_apis/x86_64 \
    --package 'system-images;android-33;google_apis;x86_64'

# Setup VNC and display
ENV DISPLAY=:1
EXPOSE 5901 5555 8080

# Copy supervisor configuration
COPY supervisord.conf /etc/supervisor/conf.d/supervisord.conf

# Create working directory for Flutter app
WORKDIR /app

# Copy startup script
COPY start-android-test.sh /start-android-test.sh
RUN chmod +x /start-android-test.sh

# Set entrypoint
CMD ["/usr/bin/supervisord", "-c", "/etc/supervisor/conf.d/supervisord.conf"]