package io.katharsis.dispatcher.controller.resource;

import com.fasterxml.jackson.databind.node.ObjectNode;
import io.katharsis.dispatcher.controller.BaseControllerTest;
import io.katharsis.queryParams.QueryParams;
import io.katharsis.request.dto.DataBody;
import io.katharsis.request.dto.RequestBody;
import io.katharsis.request.dto.ResourceRelationships;
import io.katharsis.request.path.JsonPath;
import io.katharsis.request.path.ResourcePath;
import io.katharsis.resource.mock.models.Memorandum;
import io.katharsis.resource.mock.models.Task;
import io.katharsis.response.BaseResponse;
import io.katharsis.response.ResourceResponse;
import org.junit.Assert;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ResourcePatchTest extends BaseControllerTest {

    private static final String REQUEST_TYPE = "PATCH";

    @Test
    public void onGivenRequestCollectionGetShouldDenyIt() {
        // GIVEN
        JsonPath jsonPath = pathBuilder.buildPath("/tasks/");
        ResourcePatch sut = new ResourcePatch(resourceRegistry, typeParser, objectMapper);

        // WHEN
        boolean result = sut.isAcceptable(jsonPath, REQUEST_TYPE);

        // THEN
        Assert.assertEquals(result, false);
    }

    @Test
    public void onGivenRequestResourceGetShouldAcceptIt() {
        // GIVEN
        JsonPath jsonPath = pathBuilder.buildPath("/tasks/1");
        ResourcePatch sut = new ResourcePatch(resourceRegistry, typeParser, objectMapper);

        // WHEN
        boolean result = sut.isAcceptable(jsonPath, REQUEST_TYPE);

        // THEN
        Assert.assertEquals(result, true);
    }

    @Test
    public void onNoBodyResourceShouldThrowException() throws Exception {
        // GIVEN
        ResourcePost sut = new ResourcePost(resourceRegistry, typeParser, objectMapper);

        // THEN
        expectedException.expect(RuntimeException.class);

        // WHEN
        sut.handle(new ResourcePath("fridges"), new QueryParams(), null, null);
    }

    @Test
    public void onGivenRequestResourceGetShouldHandleIt() throws Exception {
        // GIVEN
        RequestBody newTaskBody = new RequestBody();
        DataBody data = new DataBody();
        newTaskBody.setData(data);
        data.setType("tasks");
        data.setAttributes(objectMapper.createObjectNode()
            .put("name", "sample task"));

        JsonPath taskPath = pathBuilder.buildPath("/tasks");

        // WHEN
        ResourcePost resourcePost = new ResourcePost(resourceRegistry, typeParser, objectMapper);
        ResourceResponse taskResponse = resourcePost.handle(taskPath, new QueryParams(), null, newTaskBody);
        assertThat(taskResponse.getData()).isExactlyInstanceOf(Task.class);
        Long taskId = ((Task) (taskResponse.getData())).getId();
        assertThat(taskId).isNotNull();

        // GIVEN
        RequestBody taskPatch = new RequestBody();
        data = new DataBody();
        taskPatch.setData(data);
        data.setType("tasks");
        data.setAttributes(objectMapper.createObjectNode()
            .put("name", "task updated"));
        JsonPath jsonPath = pathBuilder.buildPath("/tasks/" + taskId);
        ResourcePatch sut = new ResourcePatch(resourceRegistry, typeParser, objectMapper);

        // WHEN
        BaseResponse<?> response = sut.handle(jsonPath, new QueryParams(), null, taskPatch);

        // THEN
        Assert.assertNotNull(response);
        assertThat(response.getData()).isExactlyInstanceOf(Task.class);
        assertThat(((Task) (response.getData())).getName()).isEqualTo("task updated");
    }

    @Test
    public void onInheritedResourceShouldUpdateInheritedResource() throws Exception {
        // GIVEN
        RequestBody memorandumBody = new RequestBody();
        DataBody data = new DataBody();
        memorandumBody.setData(data);
        data.setType("memoranda");
        ObjectNode attributes = objectMapper.createObjectNode()
            .put("title", "sample title")
            .put("body", "sample body");
        data.setAttributes(attributes);

        JsonPath documentsPath = pathBuilder.buildPath("/documents");

        ResourcePost resourcePost = new ResourcePost(resourceRegistry, typeParser, objectMapper);

        // WHEN
        ResourceResponse taskResponse = resourcePost.handle(documentsPath, new QueryParams(), null, memorandumBody);

        // THEN
        assertThat(taskResponse.getData()).isExactlyInstanceOf(Memorandum.class);
        Long memorandumId = ((Memorandum) (taskResponse.getData())).getId();
        assertThat(memorandumId).isNotNull();

        // --------------------------

        // GIVEN
        memorandumBody = new RequestBody();
        data = new DataBody();
        memorandumBody.setData(data);
        data.setType("memoranda");
        data.setAttributes(objectMapper.createObjectNode()
            .put("title", "new title")
            .put("body", "new body"));
        JsonPath documentPath = pathBuilder.buildPath("/documents/" + memorandumId);
        ResourcePatch sut = new ResourcePatch(resourceRegistry, typeParser, objectMapper);

        // WHEN
        BaseResponse memorandumResponse = sut.handle(documentPath, new QueryParams(), null, memorandumBody);

        // THEN
        assertThat(memorandumResponse.getData()).isExactlyInstanceOf(Memorandum.class);
        Memorandum persistedMemorandum = (Memorandum) (memorandumResponse.getData());
        assertThat(persistedMemorandum.getId()).isNotNull();
        assertThat(persistedMemorandum.getTitle()).isEqualTo("new title");
        assertThat(persistedMemorandum.getBody()).isEqualTo("new body");
    }

    @Test
    public void onResourceRelationshipNullifiedShouldSaveIt() throws Exception {
        // GIVEN
        RequestBody newTaskBody = new RequestBody();
        DataBody data = new DataBody();
        newTaskBody.setData(data);
        data.setType("tasks");
        data.setAttributes(objectMapper.createObjectNode()
            .put("name", "sample task"));

        JsonPath taskPath = pathBuilder.buildPath("/tasks");

        // WHEN
        ResourcePost resourcePost = new ResourcePost(resourceRegistry, typeParser, objectMapper);
        ResourceResponse taskResponse = resourcePost.handle(taskPath, new QueryParams(), null, newTaskBody);
        assertThat(taskResponse.getData()).isExactlyInstanceOf(Task.class);
        Long taskId = ((Task) (taskResponse.getData())).getId();
        assertThat(taskId).isNotNull();

        // GIVEN
        RequestBody taskPatch = new RequestBody();
        data = new DataBody();
        taskPatch.setData(data);
        data.setType("tasks");
        data.setAttributes(objectMapper.createObjectNode()
            .put("name", "task updated"));
        data.setRelationships(new ResourceRelationships());
        data.getRelationships()
            .setAdditionalProperty("project", null);
        JsonPath jsonPath = pathBuilder.buildPath("/tasks/" + taskId);
        ResourcePatch sut = new ResourcePatch(resourceRegistry, typeParser, objectMapper);

        // WHEN
        BaseResponse<?> response = sut.handle(jsonPath, new QueryParams(), null, taskPatch);

        // THEN
        Assert.assertNotNull(response);
        assertThat(response.getData()).isExactlyInstanceOf(Task.class);
        assertThat(((Task) (response.getData())).getName()).isEqualTo("task updated");
        assertThat(((Task) (response.getData())).getProject()).isNull();
    }
}
