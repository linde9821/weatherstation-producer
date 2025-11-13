git pull

sudo systemctl stop homekit-sensor

export JAVA_HOME=/home/pi/.sdkman/candidates/java/current
export PATH=/home/pi/.sdkman/candidates/java/current/bin:$PATH

./gradlew installDist

sudo systemctl start homekit-sensor