import argparse
import json
import time

import bme280
import smbus2
from time import sleep
from kafka import KafkaProducer
from kafka.errors import KafkaError
from kafka.producer.future import FutureRecordMetadata


def parse_arguments():
    parser = argparse.ArgumentParser(description="Weather Station Kafka Producer")
    parser.add_argument(
        "--broker-url",
        type=str,
        required=True,
        help="Kafka broker URL (e.g., 'localhost:9092' or 'broker:9092')."
    )
    parser.add_argument(
        "--location",
        type=str,
        required=True,
        help="Location identifier for the weather station (e.g., 'station-1')."
    )
    parser.add_argument(
        "--interval",
        type=float,
        default=1.0,
        help="Interval (in seconds) between sensor readings. Default is 1 second."
    )
    parser.add_argument(
        "--auth",
        type=str,
        help="Optional Kafka authentication credentials in the format 'username:password'."
    )
    return parser.parse_args()


def send_message(producer: KafkaProducer, humidity: float, pressure: float, ambient_temperature: float,
                 current_time: int, location: str):
    try:
        message = {
            "timestamp": current_time,
            "location": location,
            "measurements": {
                "temperature": ambient_temperature,
                "humidity": humidity,
                "pressure": pressure
            }
        }

        # Send the message and get a future object
        future: FutureRecordMetadata = producer.send(
            topic="raw-weather-data",
            value=message,
            key=location.encode('utf-8'),
        )

        # Block until the message is sent successfully or a timeout occurs
        record_metadata = future.get(timeout=30)
        print(f"Message sent successfully to topic {record_metadata.topic}, partition {record_metadata.partition}, "
              f"offset {record_metadata.offset}")

    except KafkaError as e:
        print(f"Failed to send message to Kafka: {e}")
    except Exception as ex:
        print(f"An unexpected error occurred while sending the message: {ex}")


if __name__ == '__main__':
    try:
        args = parse_arguments()

        print('Starting weatherstation Producer\n')
        print('Connecting to bme280\n')
        port = 1
        address = 0x77  # Adafruit BME280 address. Other BME280s may be different
        bus = smbus2.SMBus(port)
        bme280.load_calibration_params(bus, address)

        print('Connecting to Kafka\n')

        # Kafka producer configuration
        producer_config = {
            'bootstrap_servers': args.broker_url,
            'value_serializer': lambda v: json.dumps(v).encode('utf-8'),
            'key_serializer': lambda k: k.encode('utf-8'),
        }

        # Add authentication if provided
        if args.auth:
            username, password = args.auth.split(":")
            producer_config['security_protocol'] = 'SASL_PLAINTEXT'
            producer_config['sasl_mechanism'] = 'PLAIN'
            producer_config['sasl_plain_username'] = username
            producer_config['sasl_plain_password'] = password

        producer: KafkaProducer = KafkaProducer(**producer_config)

        location: str = args.location
        interval: float = args.interval

        print('Starting loop\n')
        while True:
            try:
                current_time: int = round(time.time() * 1000)
                bme280_data = bme280.sample(bus, address)
                humidity: float = bme280_data.humidity
                pressure: float = bme280_data.pressure
                ambient_temperature: float = bme280_data.temperature

                print(f"Current time: {current_time}")
                print(f"Humidity: {humidity}")
                print(f"Pressure: {pressure}")
                print(f"Ambient temperature: {ambient_temperature}")

                # Send data to Kafka
                send_message(producer, humidity, pressure, ambient_temperature, current_time, location)

            except OSError as sensor_error:
                print(f"Error accessing BME280 sensor: {sensor_error}")
            except Exception as ex:
                print(f"An unexpected error occurred in the main loop: {ex}")

            # Wait for the specified interval
            sleep(interval)

    except KeyboardInterrupt:
        print("\nProducer shutting down gracefully...")

    except Exception as e:
        print(f"Failed to initialize producer or sensor: {e}")

    finally:
        if 'producer' in locals() and producer is not None:
            print("Closing Kafka producer...")
            producer.close()
        if 'bus' in locals():
            bus.close()
        print("Shutdown complete.")