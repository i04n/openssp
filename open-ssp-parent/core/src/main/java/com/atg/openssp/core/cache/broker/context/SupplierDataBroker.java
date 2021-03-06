package com.atg.openssp.core.cache.broker.context;

import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atg.openssp.common.cache.broker.AbstractDataBroker;
import com.atg.openssp.common.demand.Supplier;
import com.atg.openssp.common.exception.EmptyHostException;
import com.atg.openssp.core.cache.type.ConnectorCache;
import com.atg.openssp.core.exchange.channel.rtb.DemandBroker;
import com.atg.openssp.core.exchange.channel.rtb.OpenRtbConnector;

import restful.context.Path;
import restful.context.PathBuilder;
import restful.exception.RestException;

/**
 * Act as broker between connector which loads the data from the webservice into a data transfer object and the cache.
 * 
 * This special data broker loads the {@see SupplierDto} data from a backend which holds the data for the supplier, also known as DSP. and put into a special
 * cache reps. key-value store {@see ConnectorCache}. It uses a {@see PathBuilder} object to store information about the endpoint which is used by the generic
 * {@see AbstractDataBroker} to connect to the remote.
 * 
 * @author André Schmer
 *
 */
public final class SupplierDataBroker extends AbstractDataBroker<SupplierDto> {

	private static final Logger log = LoggerFactory.getLogger(SupplierDataBroker.class);

	public SupplierDataBroker() {}

	@Override
	public boolean doCaching() {
		try {
			final SupplierDto dto = super.connect(SupplierDto.class);
			if (dto != null) {
				log.info("sizeof supplier data=" + dto.getData().size());
				dto.getData().forEach(new Consumer<Supplier>() {
					@Override
					public void accept(final Supplier supplier) {
						final OpenRtbConnector openRtbConnector = new OpenRtbConnector(supplier.getEndPoint());
						final DemandBroker broker = new DemandBroker(supplier, openRtbConnector);
						ConnectorCache.instance.add(broker);
					}
				});
				return true;
			}
			log.error("no Supplier data");
		} catch (final RestException | EmptyHostException e) {
			log.error(e.getMessage());
		}
		return false;
	}

	@Override
	public PathBuilder getRestfulContext() {
		return getDefaulPathBuilder().addPath(Path.CORE).addPath(Path.SUPPLIER);
	}

	@Override
	protected void finalWork() {
		// need to switch the intermediate cache to make the data available
		ConnectorCache.instance.switchCache();
	}

}
