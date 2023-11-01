# User Endpoints
GET: http://localhost:8081/microuser/api/users/mail
GET ALL: http://localhost:8081/microuser/api/users/
SAVE: http://localhost:8081/microuser/api/users/
DELETE: http://localhost:8081/microuser/api/users/id
UPDATE: http://localhost:8081/microuser/api/users/id

USER JSON EXAMPLE:

{
"name":"Lucas",
"surname": "Amendola",
"mail": "lucasamendola@gmail.com",
"password": "AyZ2001",
"phoneNumber": "2494245153",
"roles":["user"]
}

# ACCOUNT ENDPOINTS

GET: http://localhost:8081/microuser/api/accounts/id
GET ALL: http://localhost:8081/microuser/api/accounts/
SAVE: http://localhost:8081/microuser/api/accounts/
DELETE: http://localhost:8081/microuser/api/accounts/id
UPDATE: http://localhost:8081/microuser/api/accounts/id

ACCOUNT JSON EXAMPLE:

{
"id":"1",
"wallet": 203985.69,
"dateUp": "2023-01-27 14:00:00"
}


# ROLE ENDPOINTS

GET ALL: http://localhost:8081/microuser/api/roles/
SAVE: http://localhost:8081/microuser/api/roles/

ROLE JSON EXAMPLE:

{
"type": "user"
}


# USERACCOUNT ENDPOINTS
GET ALL: http://localhost:8081/microuser/api/users/accounts/
GET BY ACCOUNT : http://localhost:8081/microuser/api/accounts/{id}/users
GET BY USER: http://localhost:8081/microuser/api/users/{id}/account
SAVE: http://localhost:8081/microuser/api/users/{id}/accounts/{idAccount}
DELETE: http://localhost:8081/microuser/api/users/{id}/accounts/{idAccount}

USERACCOUNT JSON EXAMPLE

{
"accountID": 11,
"userID": 18
}