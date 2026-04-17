package com.eventra.eventra.dto;

import java.util.List;
import java.util.Map;

public class AnalyticsDTO {
    
    private long totalEvents;
    private long totalRegistrations;
    private long totalUsers;
    private long totalStudents;
    private long totalClubHeads;
    private double totalRevenue;
    private Map<String, Long> monthlyRegistrations;
    private Map<String, Long> eventsByStatus;
    private List<Map<String, Object>> topEvents;
    private Map<String, Long> dailyRegistrations;

    // Constructors
    public AnalyticsDTO() {
    }

    public AnalyticsDTO(long totalEvents, long totalRegistrations, long totalUsers,
                       long totalStudents, long totalClubHeads, double totalRevenue) {
        this.totalEvents = totalEvents;
        this.totalRegistrations = totalRegistrations;
        this.totalUsers = totalUsers;
        this.totalStudents = totalStudents;
        this.totalClubHeads = totalClubHeads;
        this.totalRevenue = totalRevenue;
    }

    // Getters and Setters
    public long getTotalEvents() {
        return totalEvents;
    }

    public void setTotalEvents(long totalEvents) {
        this.totalEvents = totalEvents;
    }

    public long getTotalRegistrations() {
        return totalRegistrations;
    }

    public void setTotalRegistrations(long totalRegistrations) {
        this.totalRegistrations = totalRegistrations;
    }

    public long getTotalUsers() {
        return totalUsers;
    }

    public void setTotalUsers(long totalUsers) {
        this.totalUsers = totalUsers;
    }

    public long getTotalStudents() {
        return totalStudents;
    }

    public void setTotalStudents(long totalStudents) {
        this.totalStudents = totalStudents;
    }

    public long getTotalClubHeads() {
        return totalClubHeads;
    }

    public void setTotalClubHeads(long totalClubHeads) {
        this.totalClubHeads = totalClubHeads;
    }

    public double getTotalRevenue() {
        return totalRevenue;
    }

    public void setTotalRevenue(double totalRevenue) {
        this.totalRevenue = totalRevenue;
    }

    public Map<String, Long> getMonthlyRegistrations() {
        return monthlyRegistrations;
    }

    public void setMonthlyRegistrations(Map<String, Long> monthlyRegistrations) {
        this.monthlyRegistrations = monthlyRegistrations;
    }

    public Map<String, Long> getEventsByStatus() {
        return eventsByStatus;
    }

    public void setEventsByStatus(Map<String, Long> eventsByStatus) {
        this.eventsByStatus = eventsByStatus;
    }

    public List<Map<String, Object>> getTopEvents() {
        return topEvents;
    }

    public void setTopEvents(List<Map<String, Object>> topEvents) {
        this.topEvents = topEvents;
    }

    public Map<String, Long> getDailyRegistrations() {
        return dailyRegistrations;
    }

    public void setDailyRegistrations(Map<String, Long> dailyRegistrations) {
        this.dailyRegistrations = dailyRegistrations;
    }
}
