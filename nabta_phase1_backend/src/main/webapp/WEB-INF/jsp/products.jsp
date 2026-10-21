<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Nabta | Product Catalogue</title>
    <style>
        * { box-sizing: border-box; }
        body { margin: 0; font-family: Arial, sans-serif; background: #f4f7f2; color: #26352a; }
        header { background: #285c3b; color: white; padding: 24px 6%; }
        header h1 { margin: 0; font-size: 30px; }
        header p { margin: 8px 0 0; color: #e1efe3; }
        main { max-width: 1100px; margin: 35px auto; padding: 0 20px; }
        .heading { display: flex; justify-content: space-between; align-items: center; gap: 12px; flex-wrap: wrap; }
        .heading h2 { margin: 0; }
        .badge { background: #e0eddf; color: #285c3b; padding: 8px 13px; border-radius: 20px; font-size: 14px; }
        .table-wrap { margin-top: 22px; overflow-x: auto; background: white; border-radius: 12px; box-shadow: 0 3px 14px #0000000b; }
        table { width: 100%; border-collapse: collapse; text-align: left; }
        th { background: #e8f0e7; color: #285c3b; }
        th, td { padding: 15px 17px; border-bottom: 1px solid #edf0eb; }
        tbody tr:hover { background: #f8fbf7; }
        .price { font-weight: bold; color: #285c3b; }
        .status { padding: 5px 10px; border-radius: 15px; font-size: 12px; background: #e0f4e5; color: #216b38; }
        .inactive { background: #fbe5e5; color: #9b2929; }
        .empty { padding: 30px; text-align: center; color: #687568; }
        footer { text-align: center; color: #718071; padding: 25px; font-size: 13px; }
    </style>
</head>
<body>
<header>
    <h1>Nabta</h1>
    <p>Product Catalogue | Java Server Pages (JSP)</p>
</header>
<main>
    <div class="heading">
        <h2>Product Catalogue</h2>
        <span class="badge">Products loaded from the database</span>
    </div>
    <div class="table-wrap">
        <table>
            <thead>
                <tr>
                    <th>ID</th>
                    <th>Product Name</th>
                    <th>Category</th>
                    <th>Price (SAR)</th>
                    <th>Stock</th>
                    <th>Status</th>
                </tr>
            </thead>
           <tbody>
                <c:forEach var="product" items="${products}">
                    <tr>
                        <td><c:out value="${product.id()}"/></td>
                        <td><c:out value="${product.name()}"/></td>
                        <td><c:out value="${product.categoryName()}"/></td>
                        <td class="price"><c:out value="${product.price()}"/></td>
                        <td><c:out value="${product.stockQuantity()}"/></td>
                        <td>
                            <c:choose>
                                <c:when test="${product.active()}">
                                    <span class="status">Active</span>
                                </c:when>
                                <c:otherwise>
                                    <span class="status inactive">Inactive</span>
                                </c:otherwise>
                            </c:choose>
                        </td>
                    </tr>
                </c:forEach>
                <c:if test="${empty products}">
                    <tr>
                        <td colspan="6" class="empty">No products found.</td>
                    </tr>
                </c:if>
            </tbody>
        </table>
    </div>
</main>
<footer>Nabta Academic Demonstration | Spring Boot + JSP + MySQL</footer>
</body>
</html>