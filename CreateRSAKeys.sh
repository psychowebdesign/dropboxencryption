#!/bin/bash
openssl genrsa -out private.pem 2048
openssl pkcs8 -topk8 -in private.pem -outform DER -out private.der -nocrypt
openssl rsa -in private.pem -pubout -outform DER -out public.der

