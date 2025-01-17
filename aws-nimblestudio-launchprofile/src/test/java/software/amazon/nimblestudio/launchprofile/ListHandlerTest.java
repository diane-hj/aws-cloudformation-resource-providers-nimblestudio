package software.amazon.nimblestudio.launchprofile;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import software.amazon.awssdk.services.nimble.NimbleClient;
import software.amazon.awssdk.services.nimble.model.LaunchProfileState;
import software.amazon.awssdk.services.nimble.model.ListLaunchProfilesRequest;
import software.amazon.awssdk.services.nimble.model.ListLaunchProfilesResponse;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ListHandlerTest extends AbstractTestBase {

    private static final String DEFAULT_STUDIO_ID = "studio-123";

    @Mock
    private NimbleClient nimbleClient;

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private ProxyClient<NimbleClient> proxyClient;

    private ListHandler handler;

    @BeforeEach
    public void setup() {
        proxy = getAmazonWebServicesClientProxy();
        nimbleClient = mock(NimbleClient.class);
        when(proxyClient.client()).thenReturn(nimbleClient);
        handler = new ListHandler();
    }

    static Stream<Arguments> testParamsForException() {
        return Utils.parametersForExceptionTests();
    }

    @Test
    public void handleRequest_SimpleSuccess() {
        // Mock the response
        Mockito.doReturn(Utils.generateListLaunchProfilesResponse(LaunchProfileState.READY)).when(proxyClient)
            .injectCredentialsAndInvokeV2(any(ListLaunchProfilesRequest.class), any());

        // Mock request
        final ResourceModel model = ResourceModel.builder()
            .studioId(DEFAULT_STUDIO_ID)
            .build();
        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
            .desiredResourceState(model)
            .build();

        // Make the LIST request
        final ProgressEvent<ResourceModel, CallbackContext> response = handler
            .handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isNull();
        assertThat(response.getResourceModels()).isEqualTo(
            Utils.generateListLaunchProfilesResponseModel(DEFAULT_STUDIO_ID)
        );
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void handleRequest_IgnoreStates() {
        // Mock the response
        ListLaunchProfilesResponse listLaunchProfilesResponse = ListLaunchProfilesResponse.builder()
            .launchProfiles(Arrays.asList(
                Utils.generateLaunchProfile(LaunchProfileState.CREATE_FAILED),
                Utils.generateLaunchProfile(LaunchProfileState.DELETED)))
            .build();
        Mockito.doReturn(listLaunchProfilesResponse).when(proxyClient)
            .injectCredentialsAndInvokeV2(any(ListLaunchProfilesRequest.class), any());

        // Mock request
        final ResourceModel model = ResourceModel.builder()
            .studioId(DEFAULT_STUDIO_ID)
            .build();
        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
            .desiredResourceState(model)
            .build();

        // Make the LIST request
        final ProgressEvent<ResourceModel, CallbackContext> response = handler
            .handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(response.getResourceModels().size()).isEqualTo(0);
    }

    @ParameterizedTest
    @MethodSource("testParamsForException")
    public void handleRequest_Failed_Exception(final Class<Throwable> thrownException,
                                               final Class<Throwable> expectedException) {
        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
            .desiredResourceState(ResourceModel.builder()
                .studioId(DEFAULT_STUDIO_ID)
                .build())
            .build();

        Mockito.doThrow(thrownException).when(proxyClient).injectCredentialsAndInvokeV2(any(), any());
        assertThrows(expectedException, () -> {
            handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);
        });
    }
}
