package com.spectralogic.ds3client.integration.test.helpers;

import com.spectralogic.ds3client.Ds3Client;
import com.spectralogic.ds3client.commands.spectrads3.*;
import com.spectralogic.ds3client.models.ChecksumType;
import com.spectralogic.ds3client.models.PoolType;

import java.io.IOException;
import java.security.SignatureException;
import java.util.UUID;

import static com.spectralogic.ds3client.integration.test.helpers.ABMTestHelper.*;

/**
 * This is a testing utility designed for creating a temporary data policy, storage domain,
 * and partition for use in the integration tests to avoid error if the BP does not currently
 * have a partition available for running the unit tests.
 */
public class TempStorageUtil {

    private static final String DATA_POLICY_NAME = "_dp";
    private static final String STORAGE_DOMAIN_NAME = "_sd";
    private static final String POOL_PARTITION_NAME = "_pp";

    /**
     * Sets up a temporary data policy with a temporary storage domain and partition
     * for use in integration tests where the user may not currently have access to
     * a partition.
     * @param testSetName The unique name of tests being run under this domain to
     *                    prevent race conditions between test setup and teardown
     */
    public static TempStorageIds setup(
            final String testSetName,
            final UUID dataPolicyId,
            final Ds3Client client) throws IOException, SignatureException {
        //Create storage domain
        final PutStorageDomainSpectraS3Response storageDomainResponse = createStorageDomain(
                testSetName + STORAGE_DOMAIN_NAME,
                client);

        //Create pool partition
        final PutPoolPartitionSpectraS3Response poolPartitionResponse = createPoolPartition(
                testSetName + POOL_PARTITION_NAME,
                PoolType.ONLINE,
                client);

        //Create storage domain member linking pool partition to storage domain
        final PutPoolStorageDomainMemberSpectraS3Response memberResponse = createPoolStorageDomainMember(
                storageDomainResponse.getStorageDomainResult().getId(),
                poolPartitionResponse.getPoolPartitionResult().getId(),
                client);
        final UUID storageDomainMemberId = memberResponse.getStorageDomainMemberResult().getId();

        //create data persistence rule
        final PutDataPersistenceRuleSpectraS3Response dataPersistenceResponse = createDataPersistenceRule(
                dataPolicyId,
                storageDomainResponse.getStorageDomainResult().getId(),
                client);
        final UUID dataPersistenceRuleId = dataPersistenceResponse.getDataPersistenceRuleResult().getDataPolicyId();

        return new TempStorageIds(storageDomainMemberId, dataPersistenceRuleId);
    }

    /**
     * Tears down the temporary test environment
     */
    public static void teardown(
            final String testSetName,
            final TempStorageIds ids,
            final Ds3Client client) throws IOException, SignatureException {
        deleteDataPersistenceRule(ids.getDataPersistenceRuleId(), client);
        deleteDataPolicy(testSetName + DATA_POLICY_NAME, client);
        deleteStorageDomainMember(ids.getStorageDomainMemberId(), client);
        deleteStorageDomain(testSetName + STORAGE_DOMAIN_NAME, client);
        deletePoolPartition(testSetName + POOL_PARTITION_NAME, client);
    }

    /**
     * Creates a Data Policy with the specified checksum type and end-to-end crc requirement
     */
    public static UUID setupDataPolicy(
            final String testSetName,
            final boolean withEndToEndCrcRequired,
            final ChecksumType.Type checksumType,
            final Ds3Client client) throws IOException, SignatureException {
        final PutDataPolicySpectraS3Response dataPolicyResponse = client.putDataPolicySpectraS3(
                new PutDataPolicySpectraS3Request(testSetName + DATA_POLICY_NAME)
                        .withEndToEndCrcRequired(withEndToEndCrcRequired)
                        .withChecksumType(checksumType).withAlwaysForcePutJobCreation(true));
        client.modifyUserSpectraS3(new ModifyUserSpectraS3Request("spectra")
                .withDefaultDataPolicyId(dataPolicyResponse.getDataPolicyResult().getId()));
        return dataPolicyResponse.getDataPolicyResult().getId();
    }
}
