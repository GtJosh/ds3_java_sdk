package com.spectralogic.ds3client;

import com.spectralogic.ds3client.commands.GetBucketRequest;
import com.spectralogic.ds3client.commands.GetBucketResponse;
import com.spectralogic.ds3client.models.Contents;
import com.spectralogic.ds3client.models.ListBucketResult;
import com.spectralogic.ds3client.networking.FailedRequestException;
import com.spectralogic.ds3client.networking.TooManyRetriesException;
import com.spectralogic.ds3client.utils.Guard;
import com.spectralogic.ds3client.utils.collections.LazyIterable;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

public class GetObjectsLoader implements LazyIterable.LazyIterableLoader<Contents> {
    private static final int DEFAULT_MAX_KEYS = 1000;

    private final Ds3Client client;
    private final String bucket;
    private final String prefix;
    private final int maxKeys;
    private final int retryCount;

    private String nextMarker;
    private boolean truncated;
    private boolean endOfInput = false;

    public GetObjectsLoader(final Ds3Client client, final String bucket, final String prefix, final String nextMarker, final int maxKeys, final int retryCount) {
        this.client = client;
        this.bucket = bucket;
        this.prefix = prefix;
        this.maxKeys = maxKeys;
        this.retryCount = retryCount;

        this.nextMarker = nextMarker;
        this.truncated = nextMarker != null;
    }

    @Override
    public List<Contents> getNextValues() {
        if (endOfInput) {
            return Collections.emptyList();
        }
        int retryAttempt = 0;
        while(true) {
            final GetBucketRequest request = new GetBucketRequest(bucket);
            request.withMaxKeys(Math.min(maxKeys, DEFAULT_MAX_KEYS));
            if (prefix != null) {
                request.withPrefix(prefix);
            }
            if (truncated) {
                request.withMarker(nextMarker);
            }

            final GetBucketResponse response;
            try {
                response = this.client.getBucket(request);
                final ListBucketResult result = response.getListBucketResult();

                truncated = result.getTruncated();
                this.nextMarker = result.getNextMarker();

                if (Guard.isStringNullOrEmpty(nextMarker) && !truncated) {
                    endOfInput = true;
                }

                return result.getObjects();
            } catch (final FailedRequestException e) {
              throw new RuntimeException("Failed to get the list of objects due to a failed request", e);
            } catch (final IOException e) {
                if (retryAttempt >= retryCount) {
                    //TODO need a proxied test to validate this retry logic
                    throw new TooManyRetriesException("Failed to get the next set of objects from the getBucket request after " + retryCount + " retries", e);
                }
                retryAttempt++;
            }
        }

    }
}
