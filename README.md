# Implementation Detail

### Assumptions
- Most constraints have been formalised from the initial acceptance criteria (refer to this [README.md](https://github.com/cko-recruitment/))
- Idempotency was a concern therefore a basic idempotency check has been implemented on payment intent prior to sending to the bank
- If the same idempotency key is provided and the payment is not in progress, we can return the payment details authorized/declined to follow idempotency protocol
- Although the persistence layer is in memory, it is good practice to avoid saving raw card details, therefore they have been encrypted via Tokenization
- Payment has been stored prior to calling the bank in case we close connection after payment is processed
- Dummy vault has been used to retrieve a dummy secret key to encrypt the card details
- Requirements state to only be able to retrieve Authorized/Declined payments on the GET request, therefore they are filtered from the result. We will not return Failed payments even if they UUID exists..
- It was unclear whether the expiryMonth and expiryYear should have been included in the GET/POST response, albeit I have added anyway
- Requirements state to not allow card details in the "future", this can be quite ambiguous to what we determine the current month to be..
- Card expiry month/year have been interpreted so that `4/2026` for example would be valid up until end of the last day of `4/2026`

### Future Improvements
- Rate limiting on the Gateway would also be ideal to deny service when a given threshold is reached
- Retry policy with backoff should be configured on RestTemplate when reaching out to the bank for resiliency
- Although we have thread safety on the concurrent hashmap, it would be nice to have the persistence of the cache and payment repository in the same locked transaction
- Sending the raw card details in to the payment-gateway poses a compliance risk, therefore it would be nice to receive the tokenized details via the request
- Tokenization also has no TTL but this process would be delegated to some secure vault that manages raw card details
- Although the payments are idempotent in nature, they are non-recoverable in failed state (new idempotent request to be created). There is also no check on change of payload if idempotency header remains the same
- Idempotency keys have no TTL therefore we cannot currently recover if a payment is stuck in_progress. We should have a retry mechanism to retry the payment and then fail if unable to process..
- Authentication should be passed into the gateway to validate the user, bearer token can be implemented at a later date
- It would be nice to decouple the payment process, so we can submit a payment "in_progress" response.. this way we can process the payment asynchronously..

### Instructions to run

- `docker-compose up` will also spin up the payment gateway which will depend on the bank simulator - this should be all thats needed to test the flow
- *Pass a valid UUID Idempotency-Key header into the POST request as this is required!*

##
##

## Instructions for candidates

This is the Java version of the Payment Gateway challenge. If you haven't already read this [README.md](https://github.com/cko-recruitment/) on the details of this exercise, please do so now.

## Requirements
- JDK 17
- Docker

## Template structure

src/ - A skeleton SpringBoot Application

test/ - Some simple JUnit tests

imposters/ - contains the bank simulator configuration. Don't change this

.editorconfig - don't change this. It ensures a consistent set of rules for submissions when reformatting code

docker-compose.yml - configures the bank simulator


## API Documentation
For documentation openAPI is included, and it can be found under the following url: **http://localhost:8090/swagger-ui/index.html**

**Feel free to change the structure of the solution, use a different library etc.**