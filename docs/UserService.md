# User Service

**User Service is responsible for User Authentication and Profiles.**

## DynamoDB Tables Schema used in User Service
### USERS Table
Users Table follows a Single Table Design to optimize for cost and speed.

**PK (Partition Key):** The partition key of the table, for user records it is USER#{user email}.

**SK (Sort Key):** The sort key of the table, for user records the SK METADATA containers the user account metadata.

**PASS (Password):** The user password, encrypted with BCrypt.

**DEACTIVED:** A attribute that identifies if a user account is actived or not (it may require verification to be actived).

**VERIFICATION_CODE:** The Verification Code send in the verification email of the user that registers.

**EXPIRES_AT:** A TTL attribute of UNIX epoch time that both identifies if the VERIFICATION_CODE has expired and DynamoDB automatically delete record.

| PK                        | SK       | PASS      | DEACTIVED    | VERIFICATION_CODE | EXPIRES_AT |
|---------------------------|----------|-----------|--------------|-------------------|------------|
| USER#unverified@bmail.com | METADATA | ENCRYPTED | NOT_VERIFIED | 536264            | 432782343  |
| USER#verified@bmail.com   | METADATA | ENCRYPTED |

### AUTH_SESSIONS Table
Contains the JWT refresh tokens used accross the application.

**SUBJECT (Partition Key):** The partition key of the table, identifies the subject that owns the session
contained in the sub property of the JWT Access and Refresh Tokens.

**SESSION_ID (Sort Key):** The sort key of the table, unique identifier of the session contained in the 
session_id property of the JWT Access and Refresh Tokens.

**REFRESH_NUMBER:** A number used to identify potential refresh token abuses contained in the
refresh_number property of the JWT Access and Refresh Tokens.

**EXPIRES_AT:** A TTL attribute of UNIX epoch time used to both identify expired refresh tokens and allow
the DynamoDB automatically delete the record.

| SUBJECT            | SESSION_ID | REFRESH_NUMBER | EXPIRES_AT |
|--------------------|------------|----------------|------------|
| USER#bob@bmail.com | 234141213  | 1              | 2423431412 |

## API
### User Registration
Registers a new user into the system, if the email is not taken then we are good, if the email is taken but not verified,
then that account details are overriden otherwise an HTTP status code CONFLICT is returned.

```
POST /users/register 
{ email, password }

DynamoDB.put("USERS").item({
    PK: "USER#" + email,
    SK, "METADATA",
    PASS, bcrypt(password),
    DEACTIVED, "NOT_VERIFIED",
    VERIFICATION_CODE, verificationCode,
    EXPIRES_AT, now().plus(10, MINUTES)
    })
    .conditionalExp("attribute_not_exists(PK) OR DEACTIVED = :notVerified")
                      
returns OK | CONFLICT
```

### Verify User Account
Verifies a user account using the verification code send into the user email, if the account 
is already verified or does not exist, or the verification code is invalid or expired then an
HTTP status code CONFLICT is returned, otherwise the account get actived.

```
PATCH /users/verify
{ email, verificationCode }

DynamoDB.update("USERS").key("PK", "USER#" + email, "SK", "METADATA")
                        .updateExp("REMOVE DEACTIVED, VERIFICATION_CODE, EXPIRES_AT")
                        .conditionalExp("attribute_exists(PK) AND DEACTIVED = :notVerified
                                        AND VERIFICATION_CODE = :verificationCode AND 
                                        EXPIRES_AT > :now")
returns OK | CONFLICT
```

### User Login
Logins a user into the system, if the credentials specified are invalid or the user account is deactived then
an HTTP status code of UNAUTHORIZED is returned, otherwise the system creates a new AUTH_SESSION identified by
the userId and a random generated session_id.

```
POST /users/login
{ email, password }

DynamoDB.get("USERS").key("PK", "USER#" + email, "SK", "METADATA")
                     .projectionExp("PASS, DEACTIVED");
 
DynamoDB.put("AUTH_SESSIONS").item({
    SUBJECT: userId,
    SESSION_ID: sessionId,
    REFRESH_N: 0,
    EXPIRES_AT: now().plus(1, DAY)
})
return OK(accessToken, refreshToken) | FORBIDDEN
```

### Refresh access token
Consumes the refresh token, and returns a new refresh and access token with the same session_id 
and subject but their refresh_n attribute incremented by one. **To identify Refresh Token reuses** the 
tokens refresh_n attribute is checked if it is the same in the DynamoDB table, if not then this session is
identified as compromised and it is invalidated. The refresh_n attribute is also used to 
**limit the refresh token that this session can get**.

```
POST /users/refresh
Authentication: User
{ accessToken, refreshToken }

DynamoDB.update("AUTH_SESSIONS").key({ 
    SUBJECT: Auth.userId,
    SESSION_ID: Auth.session_id 
}).updateExp("SET REFRESH_N = :newRefreshN")
    .conditionExp("attribute_exists(SESSION_ID) AND REFRESH_N = :expectedRefreshN AND EXPIRES_AT > :now")
```

### User Logout
When a use logouts we delete the session.

```
POST /users/logout
Authentication: User

DynamoDB.delete("AUTH_SESSIONS").key("PK", Auth.userId, "SK", Auth.session_id)
```


