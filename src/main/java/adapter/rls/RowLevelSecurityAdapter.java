package adapter.rls;

import java.util.logging.Logger;

import com.exasol.ExaMetadata;
import com.exasol.adapter.AdapterException;
import com.exasol.adapter.VirtualSchemaAdapter;
import com.exasol.adapter.request.CreateVirtualSchemaRequest;
import com.exasol.adapter.request.DropVirtualSchemaRequest;
import com.exasol.adapter.request.GetCapabilitiesRequest;
import com.exasol.adapter.request.PushDownRequest;
import com.exasol.adapter.request.RefreshRequest;
import com.exasol.adapter.request.SetPropertiesRequest;
import com.exasol.adapter.response.CreateVirtualSchemaResponse;
import com.exasol.adapter.response.DropVirtualSchemaResponse;
import com.exasol.adapter.response.GetCapabilitiesResponse;
import com.exasol.adapter.response.PushDownResponse;
import com.exasol.adapter.response.RefreshResponse;
import com.exasol.adapter.response.SetPropertiesResponse;

public class RowLevelSecurityAdapter implements VirtualSchemaAdapter {
    private static final Logger LOGGER = Logger.getLogger(RowLevelSecurityAdapter.class.getName());

    @Override
    public CreateVirtualSchemaResponse createVirtualSchema(final ExaMetadata metadata,
            final CreateVirtualSchemaRequest request) throws AdapterException {
        LOGGER.fine(
                () -> "Received request to create RLS protected schema \"" + request.getVirtualSchemaName() + "\".");
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public DropVirtualSchemaResponse dropVirtualSchema(final ExaMetadata metadata,
            final DropVirtualSchemaRequest request) throws AdapterException {
        logDropVirtualSchemaRequestReceived(request);
        return DropVirtualSchemaResponse.builder().build();
    }

    protected void logDropVirtualSchemaRequestReceived(final DropVirtualSchemaRequest request) {
        LOGGER.fine(() -> "Received request to drop RLS protected schema \"" + request.getVirtualSchemaName() + "\".");
    }

    @Override
    public RefreshResponse refresh(final ExaMetadata metadata, final RefreshRequest request) throws AdapterException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public SetPropertiesResponse setProperties(final ExaMetadata metadata, final SetPropertiesRequest request)
            throws AdapterException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public GetCapabilitiesResponse getCapabilities(final ExaMetadata metadata, final GetCapabilitiesRequest request)
            throws AdapterException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public PushDownResponse pushdown(final ExaMetadata metadata, final PushDownRequest request)
            throws AdapterException {
        // TODO Auto-generated method stub
        return null;
    }
}