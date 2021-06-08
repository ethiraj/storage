package org.opengroup.osdu.storage.provider.mongodb.config;

import de.flapdoodle.embed.mongo.MongodStarter;
import de.flapdoodle.embed.mongo.config.IMongodConfig;
import de.flapdoodle.embed.mongo.config.MongodConfigBuilder;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.process.runtime.Network;

public class MongoEmbeddedConfig {

    private static final int PORT = 27019;
    private static boolean runStatus = false;

    public static synchronized void init() {
        if (runStatus) {
            return;
        }
        try {
            IMongodConfig config = new MongodConfigBuilder()
                    .net(new Net(PORT, Network.localhostIsIPv6()))
                    .version(Version.V4_0_2)
                    .build();

            MongodStarter.getDefaultInstance().prepare(config).start();
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
        runStatus = true;
    }

}
