package com.example.accountmgmt.dao.impl;

import com.example.accountmgmt.dao.AuditLogDAO;
import com.example.accountmgmt.hibernate.model.AuditLog;
import org.hibernate.Query;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

/**
 * 稽核 DAO 實作。動態組 HQL 支援多條件過濾（#9 admin log filter）。
 */
public class AuditLogDAOImpl implements AuditLogDAO {

    @Autowired
    private SessionFactory sessionFactory;

    @Override
    @Transactional
    public void addAuditLog(AuditLog log) {
        sessionFactory.getCurrentSession().save(log);
    }

    @Override
    @Transactional
    @SuppressWarnings("unchecked")
    public List<AuditLog> query(String actor, String action, String targetAccountNo,
                                Boolean success, String errorCode, Date from, Date to) {
        StringBuilder hql = new StringBuilder("from AuditLog a where 1=1");
        if (actor != null && !actor.trim().isEmpty()) {
            hql.append(" and a.actor = :actor");
        }
        if (action != null && !action.trim().isEmpty()) {
            hql.append(" and a.action = :action");
        }
        if (targetAccountNo != null && !targetAccountNo.trim().isEmpty()) {
            hql.append(" and a.targetAccountNo = :target");
        }
        if (success != null) {
            hql.append(" and a.success = :success");
        }
        if (errorCode != null && !errorCode.trim().isEmpty()) {
            hql.append(" and a.errorCode = :errorCode");
        }
        if (from != null) {
            hql.append(" and a.createdAt >= :from");
        }
        if (to != null) {
            hql.append(" and a.createdAt <= :to");
        }
        hql.append(" order by a.createdAt desc");

        Query q = sessionFactory.getCurrentSession().createQuery(hql.toString());
        if (actor != null && !actor.trim().isEmpty()) {
            q.setParameter("actor", actor.trim());
        }
        if (action != null && !action.trim().isEmpty()) {
            q.setParameter("action", action.trim());
        }
        if (targetAccountNo != null && !targetAccountNo.trim().isEmpty()) {
            q.setParameter("target", targetAccountNo.trim());
        }
        if (success != null) {
            q.setParameter("success", success);
        }
        if (errorCode != null && !errorCode.trim().isEmpty()) {
            q.setParameter("errorCode", errorCode.trim());
        }
        if (from != null) {
            q.setParameter("from", from);
        }
        if (to != null) {
            q.setParameter("to", to);
        }
        return (List<AuditLog>) q.list();
    }
}
