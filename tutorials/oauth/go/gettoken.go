//Copyright (c) Microsoft Corporation. All rights reserved.
//Copyright 2016 Confluent Inc.
//Licensed under the MIT License.
//Licensed under the Apache License, Version 2.0
//
//Original Confluent sample modified for use with Azure Event Hubs for Apache Kafka Ecosystems

package main

import (
	"encoding/base64"
	"encoding/json"
	"fmt"
	"os"
	"reflect"
	"strings"
	"time"

	"github.com/Azure/go-autorest/autorest/adal"
)

// Claims is the main container for our body information
type Claims map[string]interface{}

func getServicePrincipalToken() (*adal.ServicePrincipalToken, error) {
	tenantID := os.Getenv("AAD_TENANT_ID")
	applicationID := os.Getenv("AAD_APPLICATION_ID")
	applicationSecret := os.Getenv("AAD_APPLICATION_SECRET")
	audience := os.Getenv("AAD_AUDIENCE")

	const activeDirectoryEndpoint = "https://login.microsoftonline.com/"
	oauthConfig, err := adal.NewOAuthConfig(activeDirectoryEndpoint, tenantID)

	callback := func(token adal.Token) error {
		// This is called after the token is acquired
		return nil
	}

	spt, err := adal.NewServicePrincipalToken(
		*oauthConfig,
		applicationID,
		applicationSecret,
		audience,
		callback)

	return spt, err
}

func getExpFromClaims(claims Claims) time.Time {
	if obj, ok := claims["exp"]; ok { /* thing exists in map */
		fmt.Println(reflect.TypeOf(obj))
		if expVal, ok := obj.(float64); ok { /* do stuff with strVal */
			return time.Unix(int64(expVal), 0)
		}
	}

	return time.Now()
}

func getClaimsFromJwt(tokenStr string) (Claims, error) {
	tokenArray := strings.Split(tokenStr, ".")

	claimsByte, err := base64.RawURLEncoding.DecodeString(tokenArray[1])
	if err != nil {
		return nil, err
	}

	var claims Claims
	err = json.Unmarshal(claimsByte, &claims)
	if err != nil {
		return nil, err
	}

	return claims, nil
}

func main() {
	fmt.Println("hello world")

	rawToken := "eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiIsIng1dCI6IkJCOENlRlZxeWFHckdOdWVoSklpTDRkZmp6dyIsImtpZCI6IkJCOENlRlZxeWFHckdOdWVoSklpTDRkZmp6dyJ9.eyJhdWQiOiJodHRwczovL2ludDdibjMwMDYtMy02YjFmNC0xNi5zZXJ2aWNlYnVzLmludDcud2luZG93cy1pbnQubmV0IiwiaXNzIjoiaHR0cHM6Ly9zdHMud2luZG93cy5uZXQvNzJmOTg4YmYtODZmMS00MWFmLTkxYWItMmQ3Y2QwMTFkYjQ3LyIsImlhdCI6MTU3NTMyODU0NSwibmJmIjoxNTc1MzI4NTQ1LCJleHAiOjE1NzUzMzI0NDUsImFpbyI6IjQyVmdZSWo1dVlOYjh1eDNyd3ZualUvTXZQSW9IZ0E9IiwiYXBwaWQiOiI1MWNlNzk0My1hMWFhLTRiYjEtYjcwOC04ZjY2NjU2YTkyNDIiLCJhcHBpZGFjciI6IjEiLCJpZHAiOiJodHRwczovL3N0cy53aW5kb3dzLm5ldC83MmY5ODhiZi04NmYxLTQxYWYtOTFhYi0yZDdjZDAxMWRiNDcvIiwib2lkIjoiMzRlOTRmODktZjNlNy00NWU4LWFjZDQtZTFjNGEzMDQ1M2JmIiwic3ViIjoiMzRlOTRmODktZjNlNy00NWU4LWFjZDQtZTFjNGEzMDQ1M2JmIiwidGlkIjoiNzJmOTg4YmYtODZmMS00MWFmLTkxYWItMmQ3Y2QwMTFkYjQ3IiwidXRpIjoidGptdGNnbTVMa2F3R3Y2d1YtRVdBQSIsInZlciI6IjEuMCJ9.UPd_5wlFftO3yy73-O1nDVMNbbmTZ1b6Tn5YfAJFnfQf9asGUfuq0ZYoGLxRme0tw3X9VJIhU9JNVfo0Io_cgk9A3Ob5f3Mj58-SHxPWjV53jl0VeYWvQs_4xZbm3cjxkZoCu_-_m6gWFoqZZNnhTLbCos6vE-D3196d4kah9jIqVHCLnQVoQJ0pVpz6cdElqUaDp98c5OIJCzojTNa7_rS4crNVIi1b58N-p9H22RRmBPBsU53tI1ZBlnIRKrMxxkd68nxEEXT3afGBXEtoc8vw-bZU6_mkXIwb_FRF_7sYmfAyyZnxxU9WxQ-1Zh54k0UlxbchAryuv_XxyNhRHw"
	claims, _ := getClaimsFromJwt(rawToken)
	exp := getExpFromClaims(claims)
	fmt.Println("Token expires at %s", exp)

	spt, err := getServicePrincipalToken()

	if err != nil {
		panic(err)
	}

	/*	tokenString := ""
		parsedToken, _ := jwt.Parse(tokenString, func(token *jwt.Token) (interface{}, error) {
			if _, ok := token.Method.(*jwt.SigningMethodHMAC); !ok {
				return nil, fmt.Errorf("There was an error")
			}
			return []byte("secret"), nil
		})

		exp := parsedToken.Claims. ["exp"].(string)*/

	// Acquire a new access token
	err = spt.Refresh()
	if err == nil {
		rawToken := spt.OAuthToken()
		fmt.Println("Token " + rawToken)

		/*claims, _ := getClaimsFromJwt(rawToken)
		exp, err := getExpFromClaims(claims)
		if err != nil {
			panic(err)
		}

		fmt.Println("Token expires at ", exp)*/
	}
}
