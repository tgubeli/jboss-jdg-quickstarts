package org.jboss.as.quickstarts.datagrid.helloworld;

import java.util.List;
import java.util.logging.Logger;

import javax.inject.Inject;

import org.apache.lucene.search.Query;
import org.infinispan.Cache;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.query.CacheQuery;
import org.infinispan.query.Search;
import org.infinispan.query.SearchManager;
import org.odata4j.producer.BaseResponse;
import org.odata4j.producer.QueryInfo;
import org.odata4j.producer.Responses;


public class Application {
	
	@Inject
    private Logger log;

    public void testQuery(DefaultCacheManager m, String key, String value, String message) {
        System.out.println("Demonstrating basic Query usage of Infinispan.");


//        Cache<String, Object> cache = m.getCache();
        
//        ChannelUtils.toJSON(arg0, arg1, arg2):
//
//        cache.put("b1", new Book("hobbit","author","editor"));
//        SearchManager qf = Search.getSearchManager(cache);
//
//        org.apache.lucene.search.Query luceneQuery = qf.buildQueryBuilderForClass(Book.class)
//                .get()
//                .phrase()
//                .onField("title")
//                .sentence("the hobbit")
//                .createQuery();
//
//        CacheQuery query = qf.getQuery(luceneQuery, Book.class);
//        List<Object> list = query.list();
        
//        Cache<String, JsonValueWrapper> cache = m.getCache();
//        JsonValueWrapper jsonValue = cache.get(key);
//
//		//JsonObject jsonObject = ChannelUtils.toJSON(key, jsonValue, cache.getName());
//        
//        QueryFactory queryFactory = Search.getQueryFactory(cache);
//        Query query = queryFactory.from(Document.class).
//                having("codigo").eq("885-181-CM17").
////                and().
////                having("CodigoEstado").eq("2").
//                toBuilder().build();
//        List<Object> list = query.list();
//        
//        System.out.println(list);
        
        
      Cache<String, CachedValue> c = m.getCache();
      
      SearchManager qf = Search.getSearchManager(c.getAdvancedCache());
      Query luceneQuery = qf.buildQueryBuilderForClass(CachedValue.class)
    		  .get().keyword()
    		  .wildcard()
    		  .onField("json")
    		  .matching(key)
    		  .createQuery();
      
//      Query luceneQuery = qf.buildQueryBuilderForClass(JsonValueWrapper.class)
//    		  .get().all().createQuery();
      
      CacheQuery cacheQuery = Search.getSearchManager(c).getQuery(luceneQuery,
    		  JsonValueWrapper.class);

      List<Object> list = cacheQuery.list();
      
//      for(Object obj : list){
//    	  message = message.concat("<br>").concat(( (JsonValueWrapper) obj ).getJson());
//      }
      
      System.out.println(list);
        
    }
    
    /**
     * Get the entry.
     * Method supports both key-value approach or query approach.
     * <p/>
     * Decision logic is driven by passed parameters (entryKey is specified, or queryInfo.filter is specified)
     * <p/>
     * [ODATA SPEC] Note that standardizeJSONresponse() functions is called for return values. Results of this function
     * will be directly returned to clients
     *
     * @param setNameWhichIsCacheName - cache name
     * @param entryKey                 - key of desired entry
     * @param queryInfo                - queryInfo object from odata4j layer
     * @return
     */
    public BaseResponse callFunctionGet(String setNameWhichIsCacheName, String entryKey,
                                        QueryInfo queryInfo) throws Exception {
        List<Object> queryResult = null;
        if (entryKey != null) {
//            // ignore query and return value directly
//            CachedValue value = (CachedValue) getCache(setNameWhichIsCacheName).get(entryKey);
//            if (value != null) {
//                log.trace("CallFunctionGet entry with key " + entryKey + " was found. Returning response with status 200.");
//
//                return Responses.infinispanResponse(EdmSimpleType.STRING, "jsonValue", standardizeJSONresponse(
//                        new StringBuilder(value.getJsonValueWrapper().getJson())).toString(), Response.Status.OK);
//            } else {
//                // no results found, clients will get 404 response
//                log.trace("CallFunctionGet entry with key " + entryKey + " was not found. Returning response with status 404.");
//
//                return Responses.infinispanResponse(null, null, null, Response.Status.NOT_FOUND);
//            }

        } else {
            // NO ENTRY KEY -- query on document store expected
            if (queryInfo.filter == null) {
                return Responses.error(new OErrorImpl("Parameter 'key' is not specified, therefore we want to get entries using query filter." +
                        " \n However, $filter is not specified as well."));
            }

            log.trace("Query report for $filter " + queryInfo.filter.toString());

            SearchManager searchManager = org.infinispan.query.Search.getSearchManager(getCache(setNameWhichIsCacheName));
            MapQueryExpressionVisitor mapQueryExpressionVisitor =
                    new MapQueryExpressionVisitor(searchManager.buildQueryBuilderForClass(CachedValue.class).get());
            mapQueryExpressionVisitor.visit(queryInfo.filter);

            // Query cache here and get results based on constructed Lucene query
            CacheQuery queryFromVisitor = searchManager.getQuery(mapQueryExpressionVisitor.getBuiltLuceneQuery(),
                    CachedValue.class);
            // pass query result to the function final response
            queryResult = queryFromVisitor.list();

            log.trace(" \n Search results (obtained from search manager," +
                    " used visitor for query translation) size:" + queryResult.size() + ":");
            for (Object one_result : queryResult) {
                log.trace(one_result);
            }

            // *********************************************************************************
            // We have set queryResult object containing list of results from querying the cache
            // Now apply other filters/order by/top/skip etc. requests

            try {
                // return first n results
                if (queryInfo.top != null) {
                    int n = queryInfo.top.intValue();
                    if (n < queryResult.size()) {
                        queryResult = queryResult.subList(0, n);
                    }
                    log.trace("TOP query filter option applied, value: " + n);
                }

                // skip first n results
                if (queryInfo.skip != null) {
                    int n = queryInfo.skip.intValue();
                    if (n < queryResult.size()) {
                        queryResult = queryResult.subList(n, queryResult.size());
                        log.trace("SKIP query filter option applied, value: " + n);
                    } else {
                        // skip all
                        queryResult = queryResult.subList(queryResult.size(), queryResult.size());
                        log.trace("SKIP query filter option applied, skipped all values as n = " +
                                n + " and results size = " + queryResult.size());
                    }
                }

            } catch (Exception e) {
                throw new Exception("TOP or SKIP query option failed: " + e.getMessage());
            }

            if (queryInfo.orderBy != null) {
                throw new NotSupportedException("orderBy is not supported yet. Planned for version 1.1.");
            }
        }

        int resultsCount = queryResult.size();
        if (resultsCount > 0) {
            StringBuilder sb = new StringBuilder();
            // build response

            if (resultsCount > 1) {
                sb.append("["); // start array of results
            }

            int counter = 0;
            for (Object one_result : queryResult) {
                counter++;
                // stack more JSON strings responses if needed
                CachedValue cv = (CachedValue) one_result;
                sb.append(cv.getJsonValueWrapper().getJson());

//                sb.append("\n"); // for better readability?

                if ((resultsCount > 1) && (resultsCount > counter)) {
                    // delimit results inside of an array, don't add "," after the last one JSON
                    sb.append(", \n");
                }
            }

            if (resultsCount > 1) {
                sb.append("]"); // end array of results
            }

            log.trace("CallFunctionGet method... returning query results in JSON format: " + standardizeJSONresponse(sb).toString());
            return Responses.infinispanResponse(EdmSimpleType.STRING, "jsonValue", standardizeJSONresponse(sb).toString(), Response.Status.OK);
        } else {
            // no results found, clients will get 404 response
            return Responses.infinispanResponse(null, null, null, Response.Status.NOT_FOUND);
        }
    }
    


    public static void main(String[] args) throws Exception {
        Application a = new Application();
       // a.testQuery();

        System.out.println("Sample complete.");
    }


}
