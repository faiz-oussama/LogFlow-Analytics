package com.example;

public class WebScrapingTester {
    public static void main(String[] args) {
        WebScrapingService service = new WebScrapingService();
        
        // Test queries
        String[] queries = {
            "how to implement JWT authentication in Java",
            "spring boot rest api example",
            "java nullpointerexception best practices",
            "java stream api tutorial",
            "spring security oauth2 implementation"
        };
        
        System.out.println("Starting WebScraping Test\n");
        System.out.println("=========================\n");
        
        for (String query : queries) {
            System.out.println("Testing query: " + query);
            System.out.println("-------------------------");
            
            try {
                var results = service.searchSolutions(query);
                System.out.println("Found " + results.size() + " results\n");
                
                for (String result : results) {
                    System.out.println(result);
                    System.out.println("-------------------------");
                }
            } catch (Exception e) {
                System.err.println("Error processing query: " + query);
                System.err.println("Error: " + e.getMessage());
                e.printStackTrace();
            }
            
            System.out.println("\n=========================\n");
            
            // Add a small delay between queries to avoid rate limiting
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
}
