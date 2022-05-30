package ru.job4j.grabber.dao;

import ru.job4j.grabber.model.Post;

import java.io.InputStream;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class PsqlStore implements Store, AutoCloseable {

    private Connection cnn;

    public PsqlStore(Properties cfg) {
        try {
            Class.forName(cfg.getProperty("jdbc.driver"));
            cnn = DriverManager.getConnection(
                cfg.getProperty("jdbc.url"),
                cfg.getProperty("jdbc.username"),
                cfg.getProperty("jdbc.password")
            );
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void save(Post post) {
        try (PreparedStatement ps = cnn
                .prepareStatement("insert into post(name, text, link, created) values (?, ?, ?, ?) on conflict (link) do nothing;")) {
            ps.setString(1, post.getTitle());
            ps.setString(2, post.getDescription());
            ps.setString(3, post.getLink());
            ps.setTimestamp(4, Timestamp.valueOf(post.getCreated()));
            ps.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public List<Post> getAll() {
        List<Post> posts = new ArrayList<>();
        try (PreparedStatement ps = cnn.prepareStatement("select * from post;")) {
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    posts.add(getPostFromRs(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return posts;
    }

    @Override
    public Post findById(int id) {
        Post founded = null;
        try (PreparedStatement ps = cnn.prepareStatement("select * from post where id=?;")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    founded = getPostFromRs(rs);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return founded;
    }

    private Post getPostFromRs(ResultSet rs) throws SQLException {
        return new Post.Builder()
                .buildId(rs.getInt("id"))
                .buildTitle(rs.getString("name"))
                .buildDescription(rs.getString("text"))
                .buildLink(rs.getString("link"))
                .buildCreated(rs.getTimestamp("created").toLocalDateTime())
                .build();
    }

    @Override
    public void close() throws Exception {
        if (cnn != null) {
            cnn.close();
        }
    }

    public static void main(String[] args) {
        Properties properties = loadProperties();
        try (PsqlStore psqlStore = new PsqlStore(properties)) {
            Post post1 = new Post.Builder()
                    .buildTitle("post1")
                    .buildDescription("description1")
                    .buildLink("link1")
                    .buildCreated(LocalDateTime.now())
                    .build();
            Post post2 = new Post.Builder()
                    .buildTitle("post2")
                    .buildDescription("description2")
                    .buildLink("link2")
                    .buildCreated(LocalDateTime.now())
                    .build();
            psqlStore.save(post1);
            psqlStore.save(post2);
            List<Post> loaded = psqlStore.getAll();
            System.out.println("Get all posts:");
            loaded.forEach(System.out::println);
            System.out.printf("Get post by id 1: %s %n", psqlStore.findById(1));
            System.out.printf("Get post by id 3: %s %n", psqlStore.findById(3));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static Properties loadProperties() {
        Properties config = null;
        try (InputStream in = PsqlStore.class.getClassLoader()
                .getResourceAsStream("app.properties")) {
            config = new Properties();
            config.load(in);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        return config;
    }
}
