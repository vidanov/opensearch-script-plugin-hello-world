package com.example;

import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.search.IndexSearcher;
import org.opensearch.common.settings.Settings;
import org.opensearch.plugins.Plugin;
import org.opensearch.plugins.ScriptPlugin;
import org.opensearch.script.FieldScript;
import org.opensearch.script.ScriptContext;
import org.opensearch.script.ScriptEngine;
import org.opensearch.script.ScoreScript;
import org.opensearch.search.lookup.SearchLookup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * Hello World Script Plugin for Amazon OpenSearch Service
 * (Java 11 compatible – no switch expressions, pattern‑matching, or ScriptDocValues).
 */
public class HelloWorldScriptPlugin extends Plugin implements ScriptPlugin {

    private static final Logger logger = LoggerFactory.getLogger(HelloWorldScriptPlugin.class);

    @Override
    public ScriptEngine getScriptEngine(Settings settings, Collection<ScriptContext<?>> contexts) {
        return new HelloWorldScriptEngine();
    }

    /* ------------------------------------------------------------------
     *                          SCRIPT ENGINE
     * ------------------------------------------------------------------ */

    public static class HelloWorldScriptEngine implements ScriptEngine {

        @Override
        public String getType() {
            return "hello_world";
        }

        @Override
        public <T> T compile(String name, String source, ScriptContext<T> ctx, Map<String, String> params) {
            if (ctx.equals(FieldScript.CONTEXT)) {
                return ctx.factoryClazz.cast(new HelloWorldFieldScriptFactory(source));
            }
            if (ctx.equals(ScoreScript.CONTEXT)) {
                return ctx.factoryClazz.cast(new GenAIScoreScriptFactory(source));
            }
            throw new IllegalArgumentException("Unsupported script context: " + ctx.name);
        }

        @Override
        public Set<ScriptContext<?>> getSupportedContexts() {
            return Set.of(FieldScript.CONTEXT, ScoreScript.CONTEXT);
        }

        @Override
        public void close() {/* nothing to close */}
    }

    /* ------------------------------------------------------------------
     *                           FIELD SCRIPT
     * ------------------------------------------------------------------ */

    public static class HelloWorldFieldScriptFactory implements FieldScript.Factory {
        private final String scriptSource;
        HelloWorldFieldScriptFactory(String scriptSource) { this.scriptSource = scriptSource; }
        @Override
        public FieldScript.LeafFactory newFactory(Map<String,Object> params, SearchLookup lookup) {
            return (ctx) -> new HelloWorldFieldScript(scriptSource, params, lookup, ctx);
        }
    }

    private static class HelloWorldFieldScript extends FieldScript {
        private final String scriptSource;
        private final Map<String,Object> params;
        HelloWorldFieldScript(String scriptSource, Map<String,Object> params, SearchLookup lookup, LeafReaderContext ctx) {
            super(params, lookup, ctx);
            this.scriptSource = scriptSource;
            this.params = params;
        }
        @Override
        public Object execute() {
            if (scriptSource.contains("hello")) {
                return "Hello, " + params.getOrDefault("name", "World") + "!";
            }
            if (scriptSource.contains("timestamp")) {
                return System.currentTimeMillis();
            }
            if (scriptSource.contains("doc_id")) {
                return "doc_" + System.identityHashCode(getDoc());
            }
            if (scriptSource.contains("random")) {
                return Math.random();
            }
            return "Hello World Script Result: " + scriptSource;
        }
    }

    /* ------------------------------------------------------------------
     *                           SCORE SCRIPT
     * ------------------------------------------------------------------ */

    public static class GenAIScoreScriptFactory implements ScoreScript.Factory {
        private final String scriptSource;
        GenAIScoreScriptFactory(String scriptSource) { this.scriptSource = scriptSource; }

        @Override
        public ScoreScript.LeafFactory newFactory(Map<String,Object> params,
                                                  SearchLookup lookup,
                                                  IndexSearcher searcher) {

            // Param helpers
            double ratingThreshold   = pDouble(params, "rating_threshold", 4.5);
            double ratingBoost       = pDouble(params, "rating_boost", 1.2);
            double priceThreshold    = pDouble(params, "price_threshold", 100.0);
            double cheapBoost        = pDouble(params, "cheap_boost", 1.1);
            double expensivePenalty  = pDouble(params, "expensive_penalty", 0.9);
            double outOfStockPenalty = pDouble(params, "out_of_stock_penalty", 0.5);
            double baseMultiplier    = pDouble(params, "base_multiplier", 1.0);
            double fallbackScore     = pDouble(params, "fallback_score", 1.0);

            double ratingW           = pDouble(params, "rating_weight", 0.4);
            double priceW            = pDouble(params, "price_weight", 0.3);
            double stockW            = pDouble(params, "stock_weight", 0.2);
            double viewsW            = pDouble(params, "views_weight", 0.1);
            double salesW            = pDouble(params, "sales_weight", 0.4);
            double reviewsW          = pDouble(params, "reviews_weight", 0.1);

            double maxPrice          = pDouble(params, "max_price", 1000.0);
            double maxViews          = pDouble(params, "max_views", 10_000.0);
            double maxSales          = pDouble(params, "max_sales", 1_000.0);
            double maxReviews        = pDouble(params, "max_reviews", 1_000.0);

            String ratingField  = pString(params, "rating_field",  "rating");
            String priceField   = pString(params, "price_field",   "price");
            String stockField   = pString(params, "stock_field",   "stock");
            String viewsField   = pString(params, "views_field",   "views");
            String salesField   = pString(params, "sales_field",   "sales");
            String reviewsField = pString(params, "reviews_field", "review_count");

            return new ScoreScript.LeafFactory() {
                @Override public boolean needs_score() { return false; }
                @Override public ScoreScript newInstance(LeafReaderContext ctx) throws IOException {
                    return new ScoreScript(params, lookup, searcher, ctx) {
                        @Override
                        public double execute(ExplanationHolder explanation) {
                            try {
                                if ("weighted_score".equals(scriptSource)) {
                                    return weightedScore();
                                } else if ("popularity_score".equals(scriptSource)) {
                                    return popularityScore();
                                } else {
                                    return customScore(); // default "custom_score"
                                }
                            } catch (Exception e) {
                                logger.warn("Score fallback", e);
                                return fallbackScore;
                            }
                        }

                        /* ---------------- helpers ---------------- */
                        private double customScore() {
                            double rating = docDouble(ratingField, 0.0);
                            double price  = docDouble(priceField,  0.0);
                            int    stock  = (int) docDouble(stockField, 0);

                            double rBoost = rating > ratingThreshold ? ratingBoost : 1.0;
                            double pBoost = price  < priceThreshold  ? cheapBoost  : expensivePenalty;
                            double sBoost = stock  == 0             ? outOfStockPenalty : 1.0;

                            return rBoost * pBoost * sBoost * baseMultiplier;
                        }

                        private double weightedScore() {
                            double rating = docDouble(ratingField, 0.0);
                            double price  = docDouble(priceField,  0.0);
                            int    stock  = (int) docDouble(stockField, 0);
                            double views  = docDouble(viewsField,  0.0);

                            double nRating = Math.min(rating / 5.0, 1.0);
                            double nPrice  = Math.max(0.0, 1.0 - (price / maxPrice));
                            double nStock  = stock > 0 ? 1.0 : 0.0;
                            double nViews  = Math.min(views / maxViews, 1.0);

                            return (nRating * ratingW) + (nPrice * priceW) + (nStock * stockW) + (nViews * viewsW);
                        }

                        private double popularityScore() {
                            double views    = docDouble(viewsField,   0.0);
                            double sales    = docDouble(salesField,   0.0);
                            double rating   = docDouble(ratingField,  0.0);
                            double reviews  = docDouble(reviewsField, 0.0);

                            double vFactor = Math.log(views   + 1) / Math.log(maxViews   + 1);
                            double sFactor = Math.log(sales   + 1) / Math.log(maxSales   + 1);
                            double rFactor = rating / 5.0;
                            double revFactor = Math.log(reviews + 1) / Math.log(maxReviews + 1);

                            return (vFactor * viewsW) + (sFactor * salesW) + (rFactor * ratingW) + (revFactor * reviewsW);
                        }

                        private double docDouble(String field, double def) {
                            try {
                                if (getDoc().containsKey(field) && !getDoc().get(field).isEmpty()) {
                                    Object v = getDoc().get(field).get(0);
                                    if (v instanceof Number) return ((Number) v).doubleValue();
                                    if (v instanceof String) return Double.parseDouble((String) v);
                                }
                            } catch (Exception ignored) {}
                            return def;
                        }
                    };
                }
            };
        }

        /* ---------------- param util ---------------- */
        private static double pDouble(Map<String,Object> m, String k, double d) {
            Object v = m.get(k);
            if (v instanceof Number) return ((Number) v).doubleValue();
            if (v instanceof String) {
                try { return Double.parseDouble((String) v); } catch (NumberFormatException ignored) {}
            }
            return d;
        }
        private static String pString(Map<String,Object> m, String k, String d) {
            Object v = m.get(k);
            return v != null ? v.toString() : d;
        }
    }
}
