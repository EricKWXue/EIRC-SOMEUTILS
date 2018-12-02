package com.someutils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.List;

import org.apache.james.mime4j.dom.BinaryBody;
import org.apache.james.mime4j.dom.Entity;
import org.apache.james.mime4j.dom.Header;
import org.apache.james.mime4j.dom.Message;
import org.apache.james.mime4j.dom.Multipart;
import org.apache.james.mime4j.dom.TextBody;
import org.apache.james.mime4j.message.DefaultMessageBuilder;
import org.apache.james.mime4j.stream.Field;

public class Mht2HtmlByMime4jUtil {
	/*使用实例
	public static void main(String[] args) {
        //MessageTree.main(new String[]{"E:\\webdb\\mystruts\\src\\main\\webapp\\mhtfile\\123,123.mht"});
        mht2Html("E:\\webdb\\mystruts\\src\\main\\webapp\\mhtfile\\123,123.mht", "E:\\webdb\\mystruts\\src\\main\\webapp\\mhtfile\\123,123.htm", "");
    }*/

	/**
     * 将 mht文件转换成 html文件
     * 
     * @param srcMht      // mht 文件的位置
     * @param targetFile    // 转换后输出的HTML的位置
     */
    public static void mht2Html(String srcMht, String targetFile, String serverPath){
        DefaultMessageBuilder builder = new DefaultMessageBuilder();
        try {
            InputStream inputStream = new FileInputStream(new File(srcMht));
            Message message = builder.parseMessage(inputStream);
            Multipart multipart = (Multipart) message.getBody();
            List<Entity> parts = multipart.getBodyParts();

            //1.取主体
            StringBuilder htmlTextStr = new StringBuilder();
            String encoding = null;
            for (Entity entity : parts) {
                if(!"text/html".equals(entity.getMimeType())){
                    continue;
                }
                //取编码
                encoding = getEncoding(entity);

                TextBody body = (TextBody)entity.getBody();
                BufferedReader br = new BufferedReader(body.getReader());
                String line = null;
                while((line = br.readLine()) != null){
                    htmlTextStr.append(line);
                }
                break;
            }
            String htmlText = htmlTextStr.toString();

            //2.取资源文件 
            //创建以mht文件名称的文件夹，主要用来保存资源文件。 
            File resourceFile = null; 
            if (parts.size() > 1) {
                String sourcePath = targetFile.substring(0, targetFile.lastIndexOf(".")) + ".files";
                resourceFile = new File(sourcePath);
                if(!resourceFile.exists()){
                    resourceFile.mkdirs();
                }
            }

            for (Entity entity : parts) {
                if("text/html".equals(entity.getMimeType())){
                    continue;
                }
                String strUrl = getResourcesUrl(entity);
                if (strUrl == null || strUrl.length() == 0)
                    continue;

                // 获取资源文件的绝对路径
                String filePath = resourceFile.getAbsolutePath() + File.separator + getName(strUrl);
                File resources = new File(filePath);

                // 保存资源文件
                BinaryBody body = (BinaryBody)entity.getBody();
                if (saveResourcesFile(resources, body.getInputStream())) {
                    // 将远程地址替换为本地地址 如图片、JS、CSS样式等等
                    String replacePath = strUrl;
                    if(strUrl.startsWith("file://")){
                        replacePath = getRelativePath(strUrl);
                    }
                    String relativePath = getRelativePath(filePath);
                    htmlText = htmlText.replaceAll(replacePath, serverPath + relativePath);
                }
            }
            //保存文件
            saveHtml(htmlText, targetFile, encoding);


        } catch (Exception e) {
            e.printStackTrace();
        }

    }
    /**
     * 获取mht文件里资源文件的URL路径
     * 
     * @param bp
     * @return
     */
    private static String getResourcesUrl(Entity entity) {
        Header header = entity.getHeader();
        Field field = header.getField("Content-Location");
        if(field == null){
            return null;
        }
        return field.getBody();
    }
    /**
     * 取文件名称
     * 
     * @param strUrl
     * @return
     */
    private static String getName(String strUrl){
        char separator = '/';

        if (strUrl.lastIndexOf(separator) < 0) {
            separator = '\\';
        }
        if(strUrl.lastIndexOf(separator) < 0){
            return null;
        }
        String filename = strUrl.substring(strUrl.lastIndexOf(separator) + 1);
        if(filename.indexOf("?") > 0){
            return filename.substring(0, filename.indexOf("?"));
        }
        return filename;
    }

    /**
     * 保存网页中的JS、图片、CSS样式等资源文件
     * 
     * @param srcFile 源文件
     * @param inputStream 输入流
     * @return
     */
    private static boolean saveResourcesFile(File srcFile, InputStream inputStream) {
        if (srcFile == null || inputStream == null) {
            return false;
        }

        BufferedInputStream in = null;
        FileOutputStream fio = null;
        BufferedOutputStream osw = null;
        try {
            in = new BufferedInputStream(inputStream);
            fio = new FileOutputStream(srcFile);
            osw = new BufferedOutputStream(new DataOutputStream(fio));
            int index = 0;
            byte[] a = new byte[1024];
            while ((index = in.read(a)) != -1) {
                osw.write(a, 0, index);
            }
            osw.flush();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            try {
                if (osw != null)
                    osw.close();
                if (fio != null)
                    fio.close();
                if (in != null)
                    in.close();
                if (inputStream != null)
                    inputStream.close();
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }
    }

    /**
     * 取相对路径
     * @param filePath
     * @return
     */
    public static String getRelativePath(String filePath) {
        char separator = '/';

        if (filePath.lastIndexOf(separator) < 0) {
            separator = '\\';
        }
        if(filePath.lastIndexOf(separator) < 0){
            return "";
        }
        String partStr = filePath.substring(0, filePath.lastIndexOf(separator));
        partStr = partStr.substring(0, partStr.lastIndexOf(separator));
        return filePath.substring(partStr.length() + 1);

    }

    /**
     * 将提取出来的html内容写入保存的路径中。
     * 
     * @param htmlTxt
     * @param htmlPath
     * @param encoding
     */
    public static boolean saveHtml(String htmlTxt, String htmlPath, String encoding) {
        //若不存在父目录，则创建
        File htmlFile = new File(htmlPath);
        if(!htmlFile.getParentFile().exists()){
            htmlFile.getParentFile().mkdirs();
        }
        try {
            Writer out = null;
            out = new OutputStreamWriter(new FileOutputStream(htmlPath, false), encoding);
            out.write(htmlTxt);
            out.close();
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    /**
     * 获取mht网页文件中内容代码的编码
     * 
     * @param entity
     * @return
     */
    private static String getEncoding(Entity entity) {
        return entity.getCharset();
    }
}
