package com.anjlab.ping.pages;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Help {

    private static final Logger logger = LoggerFactory.getLogger(Help.class);
    
    private static final EntityManagerFactory emfInstance =
            Persistence.createEntityManagerFactory("transactions-optional");
    
    public void onActivate() {
        
        EntityManager em = emfInstance.createEntityManager();
        try {
            logger.warn("em = " + em);
        } finally {
            em.close();
        }
    }
    
}
