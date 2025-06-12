# Hello World Script Plugin for Amazon OpenSearch Service

This plugin enables **custom field** and **score scripts** using a custom `hello_world` language. 

It targets **OpenSearch 2.15+** and demonstrates script plugin development for **Amazon OpenSearch Service**.

## Features

- Custom script engine for field & score contexts
- Scoring logic with parameter validation
- Support for fallback scores and A/B testing
- Logging of scoring internals
- Safe field access with error handling

## Project Structure

```
genai-script-plugin/
├── src/main/java/com/example/HelloWorldScriptPlugin.java
├── plugin-descriptor.properties
├── build.gradle / gradle.properties
└── test/...
```

## Build Instructions

**Prerequisites**:

- Java 11+
- Gradle 7+
- OpenSearch 2.15 or 2.17 (custom plugins not supported on 2.19, status: June 2025)

**Steps**:

```bash
git clone <repo>
cd genai-script-plugin
./gradlew build
./gradlew test
```

ZIP will be at `build/distributions/`.

## Install to Amazon OpenSearch Service

Ensure:

- TLS 1.2+
- HTTPS, encryption at rest, and node-to-node encryption enabled

```bash
# Upload to S3
aws s3 cp build/distributions/hello-world-genai-script-plugin.zip s3://your-bucket/plugins/

# Create package
aws opensearch create-package \
  --package-name hello-world-genai-script-plugin \
  --package-type ZIP-PLUGIN \
  --package-source S3BucketName=genai-plugin-bucket,S3Key=plugins/hello-world-genai-script-plugin.zip \
  --engine-version OpenSearch_2.15 \
  --region <YOUR_AWS_REGION>

# Wait till the package is validated, associate
aws opensearch associate-package \
    --package-id <PACKAGE_ID> \
    --domain-name <OPENSEARCH_DOMAIN_NAME> \
    --region <YOUR_AWS_REGION>

# Verify
aws opensearch list-packages-for-domain --domain-name <OPENSEARCH_DOMAIN_NAME>
```

> Plugin installation triggers **blue/green deployment**—no downtime but takes time.

## Usage Examples

```json

GET _search
{
  "size":1,
  "query": {
    "match_all": {}
  },
  "script_fields": {
    "greeting": {
      "script": {
        "lang": "hello_world",
        "source": "hello",
        "params": {
          "name": "GenAI"
        }
      }
    }
  }
}


PUT products_test
{
  "settings": {
    "index": {
      "number_of_shards": 1,
      "number_of_replicas": 0
    }
  },
  "mappings": {
    "properties": {
      "name": {
        "type": "text"
      },
      "rating": {
        "type": "float"
      },
      "price": {
        "type": "float"
      },
      "stock": {
        "type": "integer"
      },
      "last_updated": {
        "type": "date"
      },
      "views": {
        "type": "integer"
      },
      "sales": {
        "type": "integer"
      }
    }
  }
}

POST products_test/_bulk?refresh=true
{"index":{}}
{"name":"Alpha Wireless Headphones","rating":4.6,"price":45.0,"stock":12,"last_updated":"2025-05-20","views":1000,"sales":150}
{"index":{}}
{"name":"Beta Noise-Cancelling Headphones","rating":4.9,"price":120.0,"stock":5,"last_updated":"2025-05-05","views":5000,"sales":400}
{"index":{}}
{"name":"Gamma Budget Earbuds","rating":3.9,"price":25.0,"stock":30,"last_updated":"2025-04-15","views":250,"sales":50}
{"index":{}}
{"name":"Delta Premium Over-Ear","rating":4.3,"price":220.0,"stock":0,"last_updated":"2025-04-01","views":3000,"sales":250}
{"index":{}}
{"name":"Epsilon Sport Earbuds","rating":4.1,"price":60.0,"stock":8,"last_updated":"2025-05-30","views":1800,"sales":300}

GET products_test/_search
{
  "query": {
    "function_score": {
      "query": {
        "match_all": {}
      },
      "script_score": {
        "script": {
          "lang": "hello_world",
          "source": "custom_score"
        }
      }
    }
  },
  "sort": [
    {
      "_score": {
        "order": "desc"
      }
    }
  ],
  "_source": [
    "name",
    "rating",
    "price",
    "stock"
  ]
}
```

## Logging

```log
INFO: Scoring: rating=4.6, price=45.0 → score=1.32
ERROR: Field 'rating' not found
```

## Development

Key classes:

- `HelloWorldScriptEngine`
- `GenAIScoreScript` & `GenAIScoreScriptFactory`
- `HelloWorldFieldScript`

To extend:

- Add new logic in `execute()`
- Update tests and docs
- Rebuild and redeploy

## Troubleshooting

- **Plugin fails to load**: Check OpenSearch version and package association
- **Errors in scripts**: Inspect logs for invalid params or missing fields

## License

This project is licensed under the [Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0).
You may use, modify, and distribute this software in accordance with the terms of the license.
