package com.boycottpro.companies;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.boycottpro.models.ResponseMessage;
import com.boycottpro.models.Users;
import com.boycottpro.utilities.JwtUtility;
import com.boycottpro.utilities.Logger;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;

import java.util.HashMap;
import java.util.Map;

public class IncrementCompanyBoycottHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final String TABLE_NAME = "companies";
    private final DynamoDbClient dynamoDb;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public IncrementCompanyBoycottHandler() {
        this.dynamoDb = DynamoDbClient.create();
    }

    public IncrementCompanyBoycottHandler(DynamoDbClient dynamoDb) {
        this.dynamoDb = dynamoDb;
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent event, Context context) {
        String sub = null;
        int lineNum = 39;
        try {
            sub = JwtUtility.getSubFromRestEvent(event);
            if (sub == null) {
            Logger.error(45, sub, "user is Unauthorized");
            return response(401, Map.of("message", "Unauthorized"));
            }
            lineNum = 46;
            Map<String, String> pathParams = event.getPathParameters();
            String companyId = (pathParams != null) ? pathParams.get("company_id") : null;
            String incrementStr = pathParams != null ? pathParams.get("increment") : null;
            if (companyId == null || companyId.isEmpty()) {
                Logger.error(51, sub, "company_id not present");
                ResponseMessage message = new ResponseMessage(400,
                        "sorry, there was an error processing your request",
                        "company_id not present");
                return response(400,message);
            }
            if (incrementStr == null || incrementStr.isEmpty()) {
                Logger.error(58, sub, "increment not present");
                ResponseMessage message = new ResponseMessage(400,
                        "sorry, there was an error processing your request",
                        "increment not present");
                return response(400,message);
            }
            if (!(incrementStr.equals("true") || incrementStr.equals("false"))) {
                Logger.error(65, sub, "increment not acceptable value");
                ResponseMessage message = new ResponseMessage(400,
                        "sorry, there was an error processing your request",
                        "increment not acceptable value");
                return response(400,message);
            }
            lineNum = 71;
            boolean increment = Boolean.parseBoolean(incrementStr);
            boolean updated = incrementCompanyRecord(companyId, increment);
            lineNum = 74;
            return response(200,"company record updated = " +
                    updated);
        } catch (Exception e) {
            Logger.error(lineNum, sub, e.getMessage());
            return response(500,Map.of("error", "Unexpected server error: " + e.getMessage()) );
        }
    }
    private APIGatewayProxyResponseEvent response(int status, Object body) {
        String responseBody = null;
        try {
            responseBody = objectMapper.writeValueAsString(body);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        return new APIGatewayProxyResponseEvent()
                .withStatusCode(status)
                .withHeaders(Map.of("Content-Type", "application/json"))
                .withBody(responseBody);
    }
    private boolean incrementCompanyRecord(String companyId, boolean increment) {
        try {
            int adjustment = increment ? 1 : -1;

            Map<String, AttributeValue> key = Map.of("company_id", AttributeValue.fromS(companyId));

            Map<String, AttributeValue> expressionAttributeValues = Map.of(
                    ":delta", AttributeValue.fromN(Integer.toString(adjustment))
            );

            UpdateItemRequest updateRequest = UpdateItemRequest.builder()
                    .tableName(TABLE_NAME)
                    .key(key)
                    .updateExpression("SET boycott_count = if_not_exists(boycott_count, :zero) + :delta")
                    .expressionAttributeValues(new HashMap<>(expressionAttributeValues) {{
                        put(":zero", AttributeValue.fromN("0"));
                    }})
                    .conditionExpression("attribute_exists(company_id)")
                    .build();

            dynamoDb.updateItem(updateRequest);
            return true;

        } catch (ConditionalCheckFailedException e) {
            System.err.println("Company not found: " + companyId);
            throw e;
        } catch (DynamoDbException e) {
            e.printStackTrace();
            throw e;
        }
    }


    private APIGatewayProxyResponseEvent response(int status, String body, String dev)  {
        ResponseMessage message = new ResponseMessage(status,body,
                dev);
        String responseBody = null;
        try {
            responseBody = objectMapper.writeValueAsString(message);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        return new APIGatewayProxyResponseEvent()
                .withStatusCode(status)
                .withHeaders(Map.of("Content-Type", "application/json"))
                .withBody(responseBody);
    }

}
