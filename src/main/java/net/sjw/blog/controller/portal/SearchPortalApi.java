package net.sjw.blog.controller.portal;


import net.sjw.blog.service.ElasticsearchService;
import net.sjw.blog.utils.R;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/portal/search")
public class SearchPortalApi {

    @Autowired
    private ElasticsearchService elasticsearchService;
    @GetMapping
    public R doSearch(@RequestParam("keyword") String keyword,
                      @RequestParam("page") int page,
                      @RequestParam("size") int size,
                      @RequestParam(value = "categoryId", required = false) String categoryId,
                      @RequestParam(value = "sort", required = false) Integer sort) {
        return elasticsearchService.doSearch(keyword, page, size, categoryId, sort);
    }

}
