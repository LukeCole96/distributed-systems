
images=$(docker-compose config | awk '/image:/ {print $2}')

for image in $images; do
    echo "Scanning image: $image"
    trivy image --severity HIGH,CRITICAL $image
done

