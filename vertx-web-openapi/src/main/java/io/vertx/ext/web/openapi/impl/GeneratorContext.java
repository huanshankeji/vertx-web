package io.vertx.ext.web.openapi.impl;

import io.vertx.core.json.JsonObject;
import io.vertx.core.json.pointer.JsonPointer;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.openapi.Operation;
import io.vertx.ext.web.validation.RequestPredicateResult;
import io.vertx.ext.web.validation.impl.validator.ValueValidator;
import io.vertx.json.schema.SchemaParser;
import io.vertx.json.schema.Draft;
import io.vertx.json.schema.JsonSchemaOptions;
import io.vertx.json.schema.impl.SchemaValidatorInternal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class GeneratorContext {

  private final SchemaParser parser;
  private final OpenAPIHolderImpl holder;
  private final Operation operation;
  private final List<Function<RoutingContext, RequestPredicateResult>> predicates;

  public GeneratorContext(SchemaParser parser, OpenAPIHolderImpl holder, Operation operation) {
    this.parser = parser;
    this.holder = holder;
    this.operation = operation;
    this.predicates = new ArrayList<>();
  }

  public JsonObject solveIfNeeded(JsonObject object) {
    return holder.solveIfNeeded(object);
  }

  public void addPredicate(Function<RoutingContext, RequestPredicateResult> predicate) {
    this.predicates.add(predicate);
  }

  public Operation getOperation() {
    return operation;
  }

  public SchemaHolder getSchemaHolder(JsonObject originalSchema, JsonObject fakeSchema, JsonPointer schemaLocation) {
    return new SchemaHolderImpl(
        originalSchema,
        schemaLocation,
        () -> normalizeSchema(originalSchema, schemaLocation),
        fakeSchema,
        (n, s) -> new ValueValidator(
          (SchemaValidatorInternal) io.vertx.json.schema.Validator.create(io.vertx.json.schema.JsonSchema.of(n), new JsonSchemaOptions().setDraft(Draft.DRAFT7).setBaseUri("app://")))
      );
  }

  public SchemaHolder getSchemaHolder(JsonObject originalSchema, JsonPointer schemaLocation) {
    return new SchemaHolderImpl(
        originalSchema,
        schemaLocation,
        () -> normalizeSchema(originalSchema, schemaLocation),
        fakeSchema(originalSchema),
        (n, s) -> new ValueValidator(
          (SchemaValidatorInternal) io.vertx.json.schema.Validator.create(io.vertx.json.schema.JsonSchema.of(n), new JsonSchemaOptions().setDraft(Draft.DRAFT7).setBaseUri("app://")))
      );
  }

  public JsonObject fakeSchema(JsonObject schema) {
    return OpenAPI3Utils.generateFakeSchema(schema, holder);
  }

  private Map.Entry<JsonPointer, JsonObject> normalizeSchema(JsonObject schema, JsonPointer schemaLocation) {
    Map<JsonPointer, JsonObject> additionalSchemas = new HashMap<>();
    Map.Entry<JsonPointer, JsonObject> normalized = holder.normalizeSchema(
      schema, schemaLocation, additionalSchemas
    );

    additionalSchemas.forEach((pointer, s) -> parser.parse(s, pointer));
    return normalized;
  }

  List<Function<RoutingContext, RequestPredicateResult>> getPredicates() {
    return predicates;
  }
}
