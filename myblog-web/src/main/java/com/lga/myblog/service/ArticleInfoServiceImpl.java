package com.lga.myblog.service;

import com.lga.myblog.bean.ArticleInfo;
import com.lga.myblog.bean.ArticleInfoBean;
import com.lga.myblog.bean.CategoryInfo;
import com.lga.myblog.bean.UserInfo;
import com.lga.myblog.dao.ArticleInfoBeanRepository;
import com.lga.myblog.dao.ArticleInfoMapper;
import com.lga.myblog.dao.CategoryInfoMapper;
import com.lga.myblog.utils.Const;
import com.lga.myblog.utils.PageBean;
import com.lga.myblog.utils.PageUtils;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.sound.sampled.Line;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

@Service
public class ArticleInfoServiceImpl implements ArticleInfoService {

    private static final Logger LOG = LoggerFactory.getLogger(ArticleInfoServiceImpl.class);

    @Autowired
    private ArticleInfoMapper articleInfoMapper;

    @Autowired
    private CategoryInfoMapper categoryInfoMapper;

    @Autowired
    private ArticleInfoBeanRepository er;

    @Override
    public int deleteArticleByCategoryId(Integer categoryId) {
        int flag = articleInfoMapper.deleteArticleByCategoryId(categoryId);
        return flag;
    }

    @Override
    public PageBean<ArticleInfo> getArticle(ArticleInfo articleInfo ,Integer page) {

        //获取总记录数
        Long allRow = articleInfoMapper.getArticleCount(articleInfo);
        //获取总页数
        Integer totalPage = (Integer) PageUtils.countTotalPage(allRow.intValue(), Const.PAGE_SIZE);
        //当前第几页
        int currentPage = PageUtils.currentPage(page);
        //起始记录数
        int start = PageUtils.countStart(Const.PAGE_SIZE, currentPage);

        if (page >= 0) {
            articleInfo.setStart(start);
            articleInfo.setLength(Const.PAGE_SIZE);
        } else {
            articleInfo.setStart(-1);
            articleInfo.setLength(-1);
        }

        List<ArticleInfo> articleInfos = articleInfoMapper.getArticle(articleInfo);

        //设置返回页面的pageBean详细信息
        PageBean<ArticleInfo> pageBean = new PageBean<>();
        pageBean.setList(articleInfos);
        pageBean.setAllRow(allRow.intValue());
        pageBean.setCurrentPage(currentPage);
        pageBean.setTotalPage(totalPage);


        return pageBean;
    }

    @Override
    @CacheEvict(cacheNames = "recomArticle", allEntries = true)
    public boolean saveArticle(ArticleInfo articleInfo) {

        if (articleInfo == null) {
            new IllegalArgumentException("添加文章失败，articleInfo对象为空");
        }
        int flag = articleInfoMapper.insertSelective(articleInfo);

        //获取插入后的articleInfo
        articleInfo = articleInfoMapper.selectByPrimaryKey(articleInfo.getArticleId());

        try {
            //保存到es中
            ArticleInfoBean articleInfoBean = new ArticleInfoBean();
            articleInfoBean.setArticleId(articleInfo.getArticleId());
            articleInfoBean.setArticleContent(articleInfo.getArticleContent());
            articleInfoBean.setArticleTitle(articleInfo.getArticleTitle());
            articleInfoBean.setArticleImg(articleInfo.getArticleImg());
            articleInfoBean.setArticleTime(articleInfo.getArticleTime());
            articleInfoBean.setCategoryName(articleInfo.getCategoryName());
            er.save(articleInfoBean);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return flag > 0 ? true : false;
    }

    @Override
    public String doPutFile(MultipartFile file) {

        try {
            //图片名称
            String fileName = file.getOriginalFilename();

            //通过时间重新构建文件名称
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
            String format = sdf.format(new Date());

            String url = Const.FILE_URL + format + fileName;
            LOG.info("上传的文件路径为:{}",url);

            //jersey客户端
            Client client = new Client();
            WebResource resource = client.resource(url);

            //将文件转为byte
            byte[] bytes = file.getBytes();
            resource.put(String.class, bytes);

            return url;

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public boolean deleteArticleById(Integer articleId) {

        if (articleId <= 0) {
            new IllegalArgumentException("articleId小于0,无法删除对应的文章");
        }
        ArticleInfo articleInfo = new ArticleInfo();
        articleInfo.setArticleId(articleId);
        articleInfo.setArticleMark("-1");
        int flag = articleInfoMapper.updateByPrimaryKeySelective(articleInfo);
        //删除es
        ArticleInfoBean articleInfoBean = new ArticleInfoBean();
        articleInfoBean.setArticleId(articleInfo.getArticleId());
        er.delete(articleInfoBean);

        return flag > 0 ? true : false;
    }

    @Override
    public ArticleInfo getArticleById(Integer articleId) {
        if (articleId <= 0) {
            new IllegalArgumentException("articleId小于0,无法删除更新的文章");
        }
        return articleInfoMapper.selectByPrimaryKey(articleId);
    }

    @Override
    @CacheEvict(cacheNames = "recomArticle" ,allEntries = true)
    public boolean updateArticle(ArticleInfo articleInfo) {
        if (articleInfo == null) {
            new IllegalArgumentException("参数articleInfo为空，所以不能进行更新操作");
        }

        int flag = 0;
        try {
            articleInfo.setArticleTime(new Date());
            flag = articleInfoMapper.updateByPrimaryKeySelective(articleInfo);
            //更新保存到es中
            ArticleInfoBean articleInfoBean = new ArticleInfoBean();
            articleInfoBean.setArticleId(articleInfo.getArticleId());
            articleInfoBean.setArticleContent(articleInfo.getArticleContent());
            articleInfoBean.setArticleTitle(articleInfo.getArticleTitle());
            articleInfoBean.setArticleImg(articleInfo.getArticleImg());
            articleInfoBean.setCategoryName(articleInfo.getCategoryName());
            articleInfoBean.setArticleTime(new Date());
            er.save(articleInfoBean);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return flag > 0 ? true : false;
    }


    @Override
    public List<ArticleInfo> getNewArticleList() {
        ArticleInfo articleInfo = new ArticleInfo();
        articleInfo.setArticleMark(Const.MARK_YES);
        articleInfo.setStart(0);
        articleInfo.setLength(10);
        List<ArticleInfo> articleInfos = articleInfoMapper.getArticle(articleInfo);
        return articleInfos;
    }

    @Override
    @Cacheable(cacheNames = "recomArticle")
    public List<ArticleInfo> getRecomArticleList(Integer categoryId) {
        ArticleInfo articleInfo = new ArticleInfo();
        articleInfo.setArticleMark(Const.MARK_YES);
        //推荐的文章
        articleInfo.setArticleRecom(Const.MARK_YES);
        articleInfo.setStart(0);
        articleInfo.setLength(10);
        if (categoryId != null && categoryId > 0) {
            articleInfo.setCategoryId(categoryId);
        }
        List<ArticleInfo> articleInfos = articleInfoMapper.getArticle(articleInfo);
        return articleInfos;
    }

    @Override
    public PageBean<ArticleInfo> getArticlesInCategoryId(CategoryInfo categoryInfo, Integer page) {

        ArticleInfo articleInfo =  new ArticleInfo();
        articleInfo.setCategoryId(categoryInfo.getCategoryId());

        //获取总记录数
        Long allRow = articleInfoMapper.getArticleCount(articleInfo);
        //获取总页数
        Integer totalPage = (Integer) PageUtils.countTotalPage(allRow.intValue(), Const.PAGE_SIZE);
        //当前第几页
        int currentPage = PageUtils.currentPage(page);
        //起始记录数
        int start = PageUtils.countStart(Const.PAGE_SIZE, currentPage);

        if (page >= 0) {
            articleInfo.setStart(start);
            articleInfo.setLength(Const.PAGE_SIZE);
        } else {
            articleInfo.setStart(-1);
            articleInfo.setLength(-1);
        }

        List<ArticleInfo> articleInfos = articleInfoMapper.getArticle(articleInfo);

        //设置返回页面的pageBean详细信息
        PageBean<ArticleInfo> pageBean = new PageBean<>();
        pageBean.setList(articleInfos);
        pageBean.setAllRow(allRow.intValue());
        pageBean.setCurrentPage(currentPage);
        pageBean.setTotalPage(totalPage);
        return pageBean;
    }

    @Override
    public Long getAllArticleCount() {
        return articleInfoMapper.getArticleCount(new ArticleInfo());
    }
}
