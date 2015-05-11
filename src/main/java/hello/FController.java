package hello;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.social.facebook.api.Album;
import org.springframework.social.facebook.api.Facebook;
import org.springframework.social.facebook.api.PagedList;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Properties;

@Controller
@EnableAutoConfiguration
@ComponentScan
@RequestMapping("/")
public class FController {

    final Properties configProp = new Properties();
    private Facebook facebook;
    @Autowired
    private FbUtils fbUtils;
    @Autowired
    private DAO dbUtils;

    @Inject
    public FController(Facebook facebook) {
        this.facebook = facebook;
    }

    @RequestMapping(method = RequestMethod.GET)
    public Object helloFacebook() {

        if (!facebook.isAuthorized()) {

            return "/connect/facebook";
        }

        return "facebookConnected";
    }

    /**
     * returns alist of albums to from Facebook
     *
     * @return
     */
    @RequestMapping(value = "/getalbums", method = RequestMethod.GET)
    public Object getAlbums() {

        if (!facebook.isAuthorized()) {

            return "redirect:/connect/facebook";
        }

        PagedList<Album> albums = facebook
                .mediaOperations()
                .getAlbums();

        return new ResponseEntity(albums, HttpStatus.OK);

    }

    /**
     * Returns a list of Photo Links for the Listed album id from the local storage
     *
     * @param albumId
     * @return ArrayList[String]
     */
    @RequestMapping(value = "/{albumId}/photos", method = RequestMethod.GET)
    public Object getPhotos(@PathVariable String albumId) {

        ArrayList<OurPhoto> ourPhotos = fbUtils.getOurPhotos(albumId);

        if (ourPhotos != null)
            return new ResponseEntity(ourPhotos, HttpStatus.OK);
        else
            return new ResponseEntity(null, HttpStatus.NO_CONTENT);
    }

    /**
     * returns albummetadata from the local storage
     *
     * @param request
     * @return
     */
    @RequestMapping(value = "/getalbummeta", method = RequestMethod.GET)
    public Object getAlbumMeta(HttpServletRequest request) {

        String albumID = request.getParameter("album_id");
        OurAlbum album = fbUtils.getAlbum(albumID);
        //Album album = facebook.mediaOperations().getAlbum(albumID);
        if (album != null)
            return new ResponseEntity(album, HttpStatus.OK);
        else
            return new ResponseEntity(null, HttpStatus.NO_CONTENT);
    }


    /**
     * backup a particular album to the personal db
     */
    @RequestMapping(value = "/backup")
    public Object backupAlbum(HttpServletRequest request) {
        String albumId = request.getParameter("album_id");
        boolean result = fbUtils.backupAlbum(albumId);

        if (result == true) {
            EmailNotification emailNotification = new EmailNotification();
            emailNotification.sendEmail(facebook.userOperations().getUserProfile().getEmail(), facebook.mediaOperations().getAlbum(albumId).getName());
            System.out.println("Mail Successfully to the user !!!");

            try {
                configProp.load(this.getClass().getClassLoader().getResourceAsStream("application.properties"));
                String tempDir = new String(configProp.getProperty("IMAGE_DIRECTORY"));
                delete(new File(tempDir + "/" + albumId + "/"));
            } catch (IOException e) {
                e.printStackTrace();
            }

            return new ResponseEntity(albumId, HttpStatus.OK);
        }
        else
            return new ResponseEntity(null, HttpStatus.NO_CONTENT);
    }

    @RequestMapping(value = "/{albumId}/photos/{photoId}", method = RequestMethod.GET)
    public Object getPhotoURL(@PathVariable String albumId, @PathVariable String photoId) {

        String imageURL = null;
        imageURL = fbUtils.getImageURL(albumId, photoId);
        if (imageURL != null)
            return new ResponseEntity(imageURL, HttpStatus.OK);
        else
            return new ResponseEntity(null, HttpStatus.NO_CONTENT);
    }



    @RequestMapping(value = "/getbackedupalbums")
    public Object getAlbumsBy(HttpServletRequest request) {
        String person_id = request.getParameter("person_id");
        ArrayList<OurAlbum> albums = fbUtils.getOurAlbumsBy(person_id);

        if (!albums.isEmpty())
            return new ResponseEntity(albums, HttpStatus.OK);
        else
            return new ResponseEntity(null, HttpStatus.NO_CONTENT);
    }

    protected void delete(File element) {
        if (element.isDirectory()) {
            for (File f : element.listFiles()) {
                this.delete(f);
            }
        }
        element.delete();
    }

}
