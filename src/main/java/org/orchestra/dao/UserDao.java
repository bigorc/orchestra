package org.orchestra.dao;

import java.util.List;

public interface UserDao {

    public List<User> findAll();

    public void createUser(User user);

    public void deleteUser(String userName);
    
    public User getUser(String userName);
    
    public void updateUser(User user);
}
