package com.piotrkalitka.placer.api;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.piotrkalitka.placer.api.dbModels.Place;
import com.piotrkalitka.placer.api.dbModels.User;

import org.hibernate.Session;
import org.hibernate.query.Query;
import org.springframework.lang.Nullable;

import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.Persistence;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

public class DataManager {

    private EntityManager entityManager = Persistence.createEntityManagerFactory("dbManager").createEntityManager();
    private Session session = (Session) entityManager.getDelegate();

    @Nullable
    public User getUser(String email) {
        CriteriaBuilder builder = session.getCriteriaBuilder();
        CriteriaQuery<User> query = builder.createQuery(User.class);
        Root<User> root = query.from(User.class);
        query.where(builder.equal(root.get("email"), email));
        query.select(root);
        Query<User> q = session.createQuery(query);
        return q.uniqueResult();
    }

    public boolean isEmailRegistered(String email) {
        return getUser(email) != null;
    }

    public void addUser(String email, String name, String surname, String password) {
        User user = new User(email, name, surname, password);
        entityManager.getTransaction().begin();
        entityManager.persist(user);
        entityManager.getTransaction().commit();
    }

    public void changePassword(int userId, String newPassword) {
        entityManager.getTransaction().begin();
        User user = entityManager.find(User.class, userId);
        user.setPassword(newPassword);
        entityManager.getTransaction().commit();
    }

    public User getUserByToken(String authToken) {
        DecodedJWT decodedJWT = JWT
                .decode(authToken);
        String email = decodedJWT.getClaim("email").asString();
        return getUser(email);
    }



    public  int addPlace(int userId, String name, String address, String website, String phoneNumber, String description) {
        Place place = new Place(userId, name, address, website, phoneNumber, description);
        entityManager.getTransaction().begin();
        entityManager.persist(place);
        entityManager.getTransaction().commit();
        return place.getId();
    }

    public List<Place> getUserPlaces(int userId) {
        CriteriaBuilder builder = session.getCriteriaBuilder();
        CriteriaQuery<Place> query = builder.createQuery(Place.class);
        Root<Place> root = query.from(Place.class);
        query.where(builder.equal(root.get("userId"), userId));
        query.select(root);
        Query<Place> q = session.createQuery(query);
        return q.list();
    }

    public List<Place> getPlaces(int limit) {
        CriteriaBuilder builder = session.getCriteriaBuilder();
        CriteriaQuery<Place> query = builder.createQuery(Place.class);
        Root<Place> root = query.from(Place.class);
        query.select(root);
        Query<Place> q = session.createQuery(query);
        q.setMaxResults(limit);
        return q.list();
    }

    public void updatePlace(int placeId, int userId, String newName, String address, String website, String phoneNumber, String description) {
        entityManager.getTransaction().begin();
        Place place = entityManager.find(Place.class, placeId);
        place.setUserId(userId);
        place.setName(newName);
        place.setAddress(address);
        place.setWebsite(website);
        place.setPhoneNumber(phoneNumber);
        place.setDescription(description);
        entityManager.getTransaction().commit();
    }

    @Nullable
    public Place getPlace(int id) {
        CriteriaBuilder builder = session.getCriteriaBuilder();
        CriteriaQuery<Place> query = builder.createQuery(Place.class);
        Root<Place> root = query.from(Place.class);
        query.where(builder.equal(root.get("id"), id));
        query.select(root);
        Query<Place> q = session.createQuery(query);
        return q.uniqueResult();
    }

    public void removePlace(int id) {
        entityManager.getTransaction().begin();
        Place place = entityManager.find(Place.class, id);
        entityManager.remove(place);
        entityManager.getTransaction().commit();
    }



    @Nullable
    public static String generateAccessToken(int userId, String email) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(Constants.JWT_SECRET);
            return JWT.create()
                    .withExpiresAt(new Date(new Date().getTime() + Constants.ACCESS_TOKEN_DURATION_MILLIS))
                    .withClaim("id", userId)
                    .withClaim("email", email)
                    .sign(algorithm);
        } catch (UnsupportedEncodingException ignore) {
            return null;
        }
    }

    @Nullable
    public static String generateRefreshToken(int userId, String email) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(Constants.JWT_SECRET);
            return JWT.create()
                    .withExpiresAt(new Date(new Date().getTime() + Constants.REFRESH_TOKEN_DURATION_MILLIS))
                    .withClaim("id", userId)
                    .withClaim("email", email)
                    .sign(algorithm);
        } catch (UnsupportedEncodingException ignore) {
            return null;
        }
    }

    public static boolean isTokenValid(String token) {
        if (token == null) return false;
        try {
            Algorithm algorithm = Algorithm.HMAC256(Constants.JWT_SECRET);
            JWTVerifier verifier = JWT.require(algorithm).build();
            verifier.verify(token);
        } catch (JWTVerificationException | UnsupportedEncodingException exception) {
            return false;
        }
        return true;
    }

    public static int getUserIdFromToken(String token) {
        DecodedJWT decodedJWT = JWT.decode(token);
        return decodedJWT.getClaim("id").asInt();
    }

}